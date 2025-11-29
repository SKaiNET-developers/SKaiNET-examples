package sk.ainet.clean.domain.usecase

import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import sk.ainet.clean.domain.port.ModelWeightsRepository

/**
 * Use case to load model weights into a classifier (PRD §5, §12).
 */
class LoadModel(
    private val repository: ModelWeightsRepository,
    private val classifier: DigitClassifier,
) {
    suspend operator fun invoke(modelId: ModelId) {
        // The classifier is responsible for applying weights internally.
        // This indirection allows for strategies that may map id→weights differently in the future.
        // For now, we simply delegate to classifier.loadModel(modelId) as per PRD example.
        classifier.loadModel(modelId)
    }
}
