package sk.ainet.clean.domain.port

import sk.ainet.clean.data.image.GrayScale28To28Image

/**
 * Domain-facing inference engine port (PRD §8).
 * Adapters will wrap concrete skainet Modules and implement this interface.
 */
interface InferenceModule {
    /** Load raw model weights. */
    fun load(weights: ByteArray)

    /** Perform inference on the provided image, returning a digit prediction 0..9. */
    fun infer(image: GrayScale28To28Image): Int
}
