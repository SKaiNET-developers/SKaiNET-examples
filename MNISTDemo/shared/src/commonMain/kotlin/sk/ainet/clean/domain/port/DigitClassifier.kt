package sk.ainet.clean.domain.port

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.domain.model.ModelId

/**
 * Domain-facing classifier capability (PRD §2).
 * Implementations should rely on [InferenceModule] and receive weights via repository/use case.
 */
interface DigitClassifier {
    /** Load model weights for the specified [modelId]. */
    suspend fun loadModel(modelId: ModelId)

    /** Classify a 28x28 grayscale image and return the predicted digit 0..9. */
    fun classify(image: GrayScale28To28Image): Int
}
