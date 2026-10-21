package sk.ainet.samples.layamoe.model

import java.io.File
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import sk.ainet.io.RandomAccessSource
import sk.ainet.io.openRandomAccessSource
import sk.ainet.samples.layamoe.platform.logEvent
import kotlinx.io.files.Path as KotlinxPath

/**
 * The checkpoints as a directory of downloaded files — the desktop cache dir or Android's
 * `filesDir`. Missing or short files are streamed from Hugging Face through the engine's
 * data-source fetcher into a `.part` file and renamed when complete; the weights are then opened
 * through the engine's file-backed random-access source.
 */
class FileModelFiles(
    private val directory: File,
    private val headers: Map<String, String> = emptyMap(),
) : ModelFiles {

    fun file(file: CheckpointFile): File = File(directory, file.relative)

    override suspend fun ensure(files: List<CheckpointFile>, onProgress: (String) -> Unit) {
        directory.mkdirs()
        val missing = files.filter { f ->
            val local = file(f)
            !local.isFile || (f.sizeBytes?.let { local.length() < it } ?: false)
        }
        if (missing.isEmpty()) return
        val fetcher = KtorRemoteDataSourceFetcher()
        try {
            for (f in missing) {
                val target = file(f).also { it.parentFile?.mkdirs() }
                val tmp = File(target.path + ".part")
                var received = 0L
                try {
                    val content = fetcher.fetch(f.url, headers)
                    val totalMb = (content.sizeBytes ?: f.sizeBytes)?.let { (it / 1_000_000).toString() } ?: "?"
                    content.source.use { source ->
                        SystemFileSystem.sink(KotlinxPath(tmp.path)).buffered().use { sink ->
                            val chunk = Buffer()
                            var lastReported = -1L
                            while (true) {
                                val n = source.readAtMostTo(chunk, 1024 * 1024)
                                if (n == -1L) break
                                sink.write(chunk, n)
                                received += n
                                val mb = received / 1_000_000
                                if (mb != lastReported) {
                                    lastReported = mb
                                    onProgress("downloading ${f.relative}… $mb / $totalMb MB")
                                }
                            }
                        }
                    }
                    check(tmp.renameTo(target)) { "rename failed: $tmp -> $target" }
                    logEvent("download_done", "file" to f.relative, "bytes" to received)
                } catch (e: Exception) {
                    tmp.delete()
                    throw e
                }
            }
        } finally {
            fetcher.close()
        }
    }

    override suspend fun text(file: CheckpointFile): String = file(file).readText()

    override fun weights(file: CheckpointFile): () -> RandomAccessSource = {
        val path = file(file).path
        openRandomAccessSource(path) ?: error("cannot open $path")
    }
}
