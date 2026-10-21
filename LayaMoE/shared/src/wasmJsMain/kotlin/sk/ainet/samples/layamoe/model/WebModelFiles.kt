@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package sk.ainet.samples.layamoe.model

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.files.Blob
import sk.ainet.io.JsBlobRandomAccessSource
import sk.ainet.io.RandomAccessSource
import sk.ainet.samples.layamoe.platform.logEvent

/**
 * The checkpoints in the browser: every file is fetched once and kept in the Cache API
 * (`skainet-layamoe`), so the next visit reads it from disk; the weights stay Blobs and the engine's
 * `JsBlobRandomAccessSource` reads the SafeTensors ranges the loader asks for through
 * `FileReaderSync` — synchronous, so only inside a Web Worker, and without a copy of the whole
 * file in wasm memory. With the Q8_0 lane the weights are packed as they are read.
 */
class WebModelFiles : ModelFiles {
    private val blobs = HashMap<String, Blob>()

    override suspend fun ensure(files: List<CheckpointFile>, onProgress: (String) -> Unit) {
        check(JsBlobRandomAccessSource.hasFileReaderSync()) { "FileReaderSync is not available: the model must be loaded inside a Web Worker" }
        for (f in files) {
            if (f.relative in blobs) continue
            val totalMb = f.sizeBytes?.let { (it / 1_000_000).toString() } ?: "?"
            var lastMb = -1L
            onProgress("fetching ${f.name}…")
            val blob = fetchCached(f.url) { received, _ ->
                val mb = (received / 1_000_000).toLong()
                if (mb != lastMb) {
                    lastMb = mb
                    onProgress("downloading ${f.relative}… $mb / $totalMb MB")
                }
            }
            val size = jsBlobSize(blob).toLong()
            if (f.sizeBytes != null && size < f.sizeBytes) error("${f.relative}: got $size bytes, expected ${f.sizeBytes}")
            logEvent("file_ready", "file" to f.relative, "bytes" to size)
            blobs[f.relative] = blob
        }
    }

    override suspend fun text(file: CheckpointFile): String = suspendCancellableCoroutine { cont ->
        jsBlobText(blob(file), { cont.resume(it) }, { cont.resumeWithException(RuntimeException(it)) })
    }

    override fun weights(file: CheckpointFile): () -> RandomAccessSource = {
        JsBlobRandomAccessSource.openSync(blob(file))
    }

    private fun blob(file: CheckpointFile): Blob = blobs[file.relative] ?: error("${file.relative} was not fetched; call ensure() first")

    private suspend fun fetchCached(url: String, onProgress: (Double, Double) -> Unit): Blob = suspendCancellableCoroutine { cont ->
        jsFetchCached(url, CACHE_NAME, onProgress, { cont.resume(it.unsafeCast<Blob>()) }, { cont.resumeWithException(RuntimeException(it)) })
    }

    companion object {
        const val CACHE_NAME = "skainet-layamoe"
    }
}

@JsFun(
    """
    (url, cacheName, onProgress, onDone, onError) => {
        (async () => {
            let cache = null;
            try { cache = await caches.open(cacheName); } catch (e) { console.warn('[layamoe] Cache API unavailable: ' + e); }
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
                    catch (e) { console.warn('[layamoe] cache put failed: ' + e); }
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
private external fun jsBlobText(blob: Blob, onDone: (String) -> Unit, onError: (String) -> Unit)

@JsFun("(blob) => blob.size")
private external fun jsBlobSize(blob: Blob): Double
