package sk.ainet.samples.emoji.model

import sk.ainet.samples.emoji.engine.LeafSource

/**
 * Resolves the LEAF checkpoint onto this platform. Desktop and Android implementations do real
 * file IO (cache hit → streamed Hugging Face download, one file at a time); the web build reads
 * the bundled Compose resources directly and needs no provider.
 */
interface LeafProvider {
    suspend fun resolve(onProgress: (String) -> Unit): LeafSource
}

/** What to do for one checkpoint file, decided without touching any IO. */
sealed interface FilePlan {
    data class UseCached(val path: String) : FilePlan
    data class Download(val path: String, val url: String) : FilePlan
}

/** Pure resolution policy: a present, complete file is reused; otherwise it is downloaded. */
object LeafResolver {
    fun plan(targetPath: String, url: String, existingBytes: Long?, expectedBytes: Long?): FilePlan = when {
        existingBytes == null -> FilePlan.Download(targetPath, url)
        expectedBytes != null && existingBytes < expectedBytes -> FilePlan.Download(targetPath, url)
        else -> FilePlan.UseCached(targetPath)
    }

    /** Streamed downloads land here first, so an interrupted download never leaves a truncated target. */
    fun partPath(targetPath: String): String = "$targetPath.part"
}
