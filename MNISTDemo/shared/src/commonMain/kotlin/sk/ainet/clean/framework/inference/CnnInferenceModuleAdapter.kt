package sk.ainet.clean.framework.inference

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.lang.model.createMNISTCNN
import sk.ainet.lang.model.classifyImageCNN
import sk.ainet.lang.model.loadWeightsFromBytes
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * CNN adapter that wraps a sk.ai.net.nn.Module and exposes the InferenceModule port (PRD §8).
 *
 * Primary constructor is testing-friendly: it accepts function delegates for load and infer.
 * Use [fromModule] to construct an adapter backed by a real CNN Module.
 */
class CnnInferenceModuleAdapter(
    private val loadFn: (weights: ByteArray) -> Unit,
    private val inferFn: (image: GrayScale28To28Image) -> Int,
) : InferenceModule {

    override fun load(weights: ByteArray) = loadFn(weights)

    override fun infer(image: GrayScale28To28Image): Int = inferFn(image)

    companion object {
        /** Create an adapter from an existing Module instance. */
        fun fromModule(module: Module<FP32, Float>): CnnInferenceModuleAdapter {
            return CnnInferenceModuleAdapter(
                loadFn = { bytes -> loadWeightsFromBytes(module, bytes) },
                inferFn = { image -> classifyImageCNN(module, image) }
            )
        }

        /** Create an adapter with a freshly built CNN Module. */
        fun create(): CnnInferenceModuleAdapter = fromModule(createMNISTCNN())
    }
}
