package sk.ainet.samples.emoji.model

import java.io.File
import sk.ainet.samples.emoji.engine.LeafSource

/**
 * Desktop has no bundled-asset concept, so this is cache-hit-or-download per file: the
 * checkpoint lands in `~/.skainet-examples/semantic-emoji-filter/leaf/` in the Hugging Face
 * layout and is reused across runs (and by the CLI).
 */
class DesktopLeafProvider(
    private val cacheDir: String = defaultLeafCacheDir(),
) : LeafProvider {
    override suspend fun resolve(onProgress: (String) -> Unit): LeafSource {
        val dir = File(cacheDir).apply { mkdirs() }
        downloadMissingLeafFiles(dir, onProgress)
        return LeafSource.Directory(dir.path)
    }
}

fun defaultLeafCacheDir(): String =
    System.getenv("LEAF_MODEL_DIR")?.takeIf { it.isNotBlank() }
        ?: File(System.getProperty("user.home"), ".skainet-examples/semantic-emoji-filter/leaf").path
