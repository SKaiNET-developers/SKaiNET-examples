@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.wasm.unsafe.UnsafeWasmMemoryApi::class)

package sk.ainet.samples.emoji.verifier

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Fetching and reading checkpoint files inside a Web Worker. A file is fetched once and kept in
 * the Cache API (`skainet-laya`), so the next visit reads it from disk instead of the network;
 * the bytes reach Kotlin through wasm linear memory (one `Uint8Array.set` per chunk, then a wasm
 * loop into the `ByteArray`), never one JS call per byte. `FileReaderSync` makes the chunk reads
 * synchronous, which is why this only runs in a worker.
 */
internal object WebFiles {
    const val CACHE_NAME = "skainet-laya"

    /** Fetches [url] (or takes it from the cache) and returns the Blob; [onProgress] gets (received, total) bytes while downloading. */
    suspend fun fetchCached(url: String, onProgress: (Double, Double) -> Unit): JsAny = suspendCancellableCoroutine { cont ->
        jsFetchCached(url, CACHE_NAME, onProgress, { cont.resume(it) }, { cont.resumeWithException(RuntimeException(it)) })
    }

    /** The whole Blob as text (the JSON side files). */
    suspend fun text(blob: JsAny): String = suspendCancellableCoroutine { cont ->
        jsBlobText(blob, { cont.resume(it) }, { cont.resumeWithException(RuntimeException(it)) })
    }

    fun size(blob: JsAny): Long = jsBlobSize(blob).toLong()

    /** The Blob as equal-sized chunks (the last one shorter), the layout `ByteArrayRandomAccessSource` wants. */
    fun chunks(blob: JsAny, chunkBytes: Int = 64 * 1024 * 1024, onProgress: (Long, Long) -> Unit = { _, _ -> }): List<ByteArray> {
        val total = size(blob)
        val out = ArrayList<ByteArray>()
        var position = 0L
        while (position < total) {
            val n = minOf(chunkBytes.toLong(), total - position).toInt()
            out += readRange(blob, position, n)
            position += n
            onProgress(position, total)
        }
        return out
    }

    private fun readRange(blob: JsAny, start: Long, length: Int): ByteArray {
        val bytes = ByteArray(length)
        var done = 0
        while (done < length) {
            val n = minOf(length - done, STAGE_BYTES)
            withScopedMemoryAllocator { allocator ->
                val base = allocator.allocate(n)
                jsReadBlobInto(blob, (start + done).toDouble(), n, wasmMemory, base.address.toInt())
                var p: Pointer = base
                for (i in 0 until n) {
                    bytes[done + i] = p.loadByte()
                    p += 1
                }
            }
            done += n
        }
        return bytes
    }

    private const val STAGE_BYTES = 16 * 1024 * 1024
}

@JsFun(
    """
    (url, cacheName, onProgress, onDone, onError) => {
        (async () => {
            let cache = null;
            try { cache = await caches.open(cacheName); } catch (e) { console.warn('[laya] Cache API unavailable: ' + e); }
            let response = cache ? await cache.match(url) : undefined;
            if (!response) {
                const net = await fetch(url);
                if (!net.ok) throw new Error('fetch failed: ' + net.status + ' ' + url);
                const total = Number(net.headers.get('Content-Length') || net.headers.get('X-Linked-Size') || 0);
                const reader = net.body.getReader();
                const parts = [];
                let received = 0;
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    parts.push(value);
                    received += value.length;
                    onProgress(received, total);
                }
                const blob = new Blob(parts);
                if (cache) {
                    try { await cache.put(url, new Response(blob, { headers: { 'Content-Type': 'application/octet-stream', 'Content-Length': String(blob.size) } })); }
                    catch (e) { console.warn('[laya] cache put failed: ' + e); }
                }
                onDone(blob);
                return;
            }
            onDone(await response.blob());
        })().catch((e) => onError(String(e && e.message ? e.message : e)));
    }
    """,
)
private external fun jsFetchCached(url: String, cacheName: String, onProgress: (Double, Double) -> Unit, onDone: (JsAny) -> Unit, onError: (String) -> Unit)

@JsFun("(blob, onDone, onError) => blob.text().then(onDone).catch((e) => onError(String(e && e.message ? e.message : e)))")
private external fun jsBlobText(blob: JsAny, onDone: (String) -> Unit, onError: (String) -> Unit)

@JsFun("(blob) => blob.size")
private external fun jsBlobSize(blob: JsAny): Double

@JsFun("(blob, start, length, kmem, ptr) => new Uint8Array(kmem.buffer).set(new Uint8Array(new FileReaderSync().readAsArrayBuffer(blob.slice(start, start + length))), ptr)")
private external fun jsReadBlobInto(blob: JsAny, start: Double, length: Int, kmem: JsAny, ptr: Int)
