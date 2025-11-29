package sk.ainet.clean.domain.strategy

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.domain.port.ModelWeightsRepository

/**
 * CNN strategy implementation of DigitClassifier (PRD §3).
 * Delegates to the provided InferenceModule and loads weights via ModelWeightsRepository.
 */
class CnnDigitClassifier(
    private val inference: InferenceModule,
    private val repository: ModelWeightsRepository,
) : DigitClassifier {

    private var loaded: Boolean = false

    override suspend fun loadModel(modelId: ModelId) {
        val bytes = repository.getWeights(modelId)
        inference.load(bytes)
        loaded = true
    }

    override fun classify(image: GrayScale28To28Image): Int {
        check(loaded) { "Model weights not loaded; call loadModel() before classify()" }
        return inference.infer(image)
    }
}
