package sk.ainet.samples.emoji.verifier

import java.io.File
import kotlin.time.TimeSource
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import sk.ainet.decisions.DecisionAdapter
import sk.ainet.models.laya.LayaDecisionAdapter
import sk.ainet.samples.emoji.platform.logEvent
import kotlinx.io.files.Path as KotlinxPath

/**
 * The checkpoint as a directory of downloaded files (desktop cache dir or Android filesDir),
 * loaded through the transformers Laya adapter. The cartridge-backed source of the future
 * implements the same [DecisionAdapterSource] and makes this class obsolete.
 */
class LayaFileSource(
    private val directory: File,
    private val headers: Map<String, String> = emptyMap(),
) : DecisionAdapterSource {
    override val modelId: String = EmojiMatchSchema.MODEL_ID

    override suspend fun open(onProgress: (String) -> Unit): DecisionAdapter {
        directory.mkdirs()
        downloadMissing(onProgress)
        onProgress("building ${modelId} (FP32, exact GELU)…")
        val started = TimeSource.Monotonic.markNow()
        val agent = loadLayaAgent(directory.path, DirectCpuExecutionContext())
        logEvent("laya_loaded", "dir" to directory.path, "loadMs" to started.elapsedNow().inWholeMilliseconds)
        return LayaDecisionAdapter(agent, modelId)
    }

    private suspend fun downloadMissing(onProgress: (String) -> Unit) {
        val missing = LayaCheckpoint.FILES.filter { file ->
            val f = File(directory, file)
            !f.isFile || (LayaCheckpoint.SIZES[file]?.let { f.length() < it } ?: false)
        }
        if (missing.isEmpty()) return
        val fetcher = KtorRemoteDataSourceFetcher()
        try {
            for (file in missing) {
                val target = File(directory, file).also { it.parentFile.mkdirs() }
                val tmp = File(target.path + ".part")
                var received = 0L
                try {
                    val content = fetcher.fetch(LayaCheckpoint.url(file), headers)
                    val totalMb = content.sizeBytes?.let { (it / 1_000_000).toString() } ?: "?"
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
                                    onProgress("downloading Laya ${target.name}… $mb / $totalMb MB")
                                }
                            }
                        }
                    }
                    check(tmp.renameTo(target)) { "rename failed: $tmp -> $target" }
                    logEvent("laya_download_done", "file" to file, "fileMB" to received / 1_000_000)
                } catch (e: Exception) {
                    tmp.delete()
                    throw e
                }
            }
        } finally {
            fetcher.close()
        }
    }
}
