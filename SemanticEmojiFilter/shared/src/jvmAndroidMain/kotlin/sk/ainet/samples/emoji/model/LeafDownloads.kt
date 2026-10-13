package sk.ainet.samples.emoji.model

import java.io.File
import kotlin.time.TimeSource
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import sk.ainet.samples.emoji.engine.LeafFiles
import sk.ainet.samples.emoji.platform.logEvent
import kotlinx.io.files.Path as KotlinxPath

/**
 * Fetches every checkpoint file that is missing or truncated in [dir], streamed straight to
 * disk through SKaiNET's Ktor fetcher: the 90 MB encoder never sits on the heap. Shared by the
 * desktop and the Android provider (both are JVM code).
 */
suspend fun downloadMissingLeafFiles(dir: File, onProgress: (String) -> Unit, headers: Map<String, String> = emptyMap()) {
    val plans = LeafFiles.ALL.map { file ->
        val target = File(dir, file)
        LeafResolver.plan(target.path, LeafFiles.url(file), target.takeIf { it.isFile }?.length(), LeafFiles.SIZES[file])
    }
    val downloads = plans.filterIsInstance<FilePlan.Download>()
    if (downloads.isEmpty()) {
        logEvent("model_cached", "dir" to dir.path)
        return
    }
    val fetcher = KtorRemoteDataSourceFetcher()
    try {
        for (plan in downloads) {
            val target = File(plan.path)
            target.parentFile.mkdirs()
            val tmp = File(LeafResolver.partPath(plan.path))
            val startedAt = TimeSource.Monotonic.markNow()
            var received = 0L
            logEvent("download_start", "url" to plan.url)
            try {
                val content = fetcher.fetch(plan.url, headers)
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
                                onProgress("Downloading ${target.name}… $mb / $totalMb MB")
                            }
                        }
                    }
                }
                check(tmp.renameTo(target)) { "rename failed: $tmp -> $target" }
                logEvent("download_done", "file" to target.name, "fileMB" to received / 1_000_000, "durMs" to startedAt.elapsedNow().inWholeMilliseconds)
            } catch (e: Exception) {
                tmp.delete()
                logEvent("download_failed", "file" to target.name, "receivedMB" to received / 1_000_000, "error" to (e.message ?: e::class.simpleName))
                throw e
            }
        }
    } finally {
        fetcher.close()
    }
}
