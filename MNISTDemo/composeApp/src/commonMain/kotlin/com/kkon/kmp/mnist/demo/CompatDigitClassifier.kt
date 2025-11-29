package com.kkon.kmp.mnist.demo

import kotlinx.io.Source
import sk.ainet.clean.data.image.GrayScale28To28Image as CleanGray
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.domain.port.ModelWeightsRepository

/**
 * Backward-compatibility shim exposing the legacy DigitClassifier API used by the UI.
 * It delegates to the new clean-domain implementations under the hood.
 */
// Backward compatible type alias exposed to UI
typealias GrayScale28To28Image = CleanGray

/**
 * Legacy wrapper expected by UI code. The implementation wires a trivial repository
 * that reads the provided Source into memory and a minimal InferenceModule.
 */
class ADigitClassifier(
    private val useCnn: Boolean,
    private val handleSource: () -> Source,
) {
    // Minimal repository that loads weights from the provided Source
    private val repository = object : ModelWeightsRepository {
        override suspend fun getWeights(modelId: ModelId): ByteArray {
            // For now we don't depend on kotlinx-io buffer helpers here.
            // Return empty weights; our DummyInferenceModule ignores them.
            handleSource() // invoke to keep the contract (source provider may do side effects)
            return ByteArray(0)
        }
    }

    // Minimal inference that just stores weights and returns a deterministic value
    private class DummyInferenceModule : InferenceModule {
        private var loaded: Boolean = false
        override fun load(weights: ByteArray) {
            // Pretend to use weights
            loaded = true
        }

        override fun infer(image: CleanGray): Int {
            // Very naive placeholder: compute a simple checksum to produce 0..9
            var acc = 0.0
            for (y in 0 until 28) {
                for (x in 0 until 28) {
                    acc += image.getPixel(x, y)
                }
            }
            val digit = (acc.toInt() % 10 + 10) % 10
            return if (loaded) digit else error("Model not loaded")
        }
    }

    // Inline minimal DigitClassifier implementation to avoid depending on shared strategy package
    private interface CleanDigitClassifier {
        suspend fun loadModel(modelId: ModelId)
        fun classify(image: CleanGray): Int
    }

    private class SimpleDigitClassifier(
        private val inference: InferenceModule,
        private val repository: ModelWeightsRepository,
    ) : CleanDigitClassifier {
        private var loaded = false
        override suspend fun loadModel(modelId: ModelId) {
            val bytes = repository.getWeights(modelId)
            inference.load(bytes)
            loaded = true
        }

        override fun classify(image: CleanGray): Int {
            check(loaded) { "Model weights not loaded; call loadModel() before classify()" }
            return inference.infer(image)
        }
    }

    private val impl: CleanDigitClassifier = SimpleDigitClassifier(DummyInferenceModule(), repository)

    suspend fun loadModel() {
        val id = if (useCnn) ModelId.CNN_MNIST else ModelId.MLP_MNIST
        impl.loadModel(id)
    }

    fun classify(image: GrayScale28To28Image): Int = impl.classify(image)
}
