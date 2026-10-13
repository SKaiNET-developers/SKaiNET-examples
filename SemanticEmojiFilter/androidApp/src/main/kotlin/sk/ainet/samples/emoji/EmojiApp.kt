package sk.ainet.samples.emoji

import android.app.Application
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.model.AndroidLeafProvider
import sk.ainet.samples.emoji.verify.Verifier

/** Application-scoped factories: the checkpoint provider and the verifier live for the app lifetime. */
class EmojiApp : Application() {
    val leafProvider by lazy { AndroidLeafProvider(this) }
    val verifier: Verifier by lazy { createVerifier(this) }

    suspend fun loadEngine(onProgress: (String) -> Unit): LeafEmbeddingEngine {
        val source = leafProvider.resolve(onProgress)
        return LeafEmbeddingEngine.load(source, onProgress = onProgress)
    }
}
