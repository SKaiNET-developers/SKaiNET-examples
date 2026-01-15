package com.kkon.kmp.mnist.demo

import kotlinx.io.Source
import kotlinx.io.readByteArray
import sk.ainet.clean.data.image.GrayScale28To28Image as CleanGray
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.domain.port.ModelWeightsRepository
import sk.ainet.clean.framework.inference.CnnInferenceModuleAdapter
import sk.ainet.clean.framework.inference.MlpInferenceModuleAdapter

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
            // Read all bytes from the source provided by the handleSource function.
            val source = handleSource()
            return source.readByteArray()
        }

        override suspend fun putWeights(modelId: ModelId, weights: ByteArray) {
            // No-op for the compatibility shim
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

    private val impl: CleanDigitClassifier = SimpleDigitClassifier(
        if (useCnn) CnnInferenceModuleAdapter.create() else MlpInferenceModuleAdapter.create(),
        repository
    )

    suspend fun loadModel() {
        val id = if (useCnn) ModelId.CNN_MNIST else ModelId.MLP_MNIST
        impl.loadModel(id)
    }

    fun classify(image: GrayScale28To28Image): Int = impl.classify(image)
}
