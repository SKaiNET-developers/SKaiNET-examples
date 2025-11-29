package sk.ainet.clean.framework.inference

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.domain.port.InferenceModule

/**
 * Minimal CNN adapter exposing the InferenceModule port (PRD §8).
 * This variant is testing-friendly and platform-agnostic: callers provide delegates.
 */
class CnnInferenceModuleAdapter(
    private val loadFn: (weights: ByteArray) -> Unit,
    private val inferFn: (image: GrayScale28To28Image) -> Int,
) : InferenceModule {

    override fun load(weights: ByteArray) = loadFn(weights)

    override fun infer(image: GrayScale28To28Image): Int = inferFn(image)
}
