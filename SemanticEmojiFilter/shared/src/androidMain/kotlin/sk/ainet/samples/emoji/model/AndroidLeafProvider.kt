package sk.ainet.samples.emoji.model

import android.content.Context
import java.io.File
import sk.ainet.samples.emoji.engine.LeafSource

/**
 * Resolves the checkpoint files into `filesDir/leaf`, streamed from the Hugging Face Hub on
 * first use; afterwards the app runs offline.
 */
class AndroidLeafProvider(
    private val context: Context,
    private val hfToken: String = "",
) : LeafProvider {
    override suspend fun resolve(onProgress: (String) -> Unit): LeafSource {
        val dir = File(context.filesDir, "leaf").apply { mkdirs() }
        val headers = hfToken.takeIf { it.isNotBlank() }?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        downloadMissingLeafFiles(dir, onProgress, headers)
        return LeafSource.Directory(dir.path)
    }
}
