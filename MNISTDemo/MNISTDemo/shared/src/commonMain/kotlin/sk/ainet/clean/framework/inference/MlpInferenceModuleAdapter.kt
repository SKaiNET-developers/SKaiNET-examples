package sk.ainet.clean.framework.inference

import com.kkon.kmp.mnist.data.image.GrayScale28To28Image
import com.kkon.kmp.mnist.model.common.classifyImage
import com.kkon.kmp.mnist.model.mlp.createMLP
import com.kkon.kmp.mnist.data.loader.loadModelWeights
import kotlinx.io.Buffer
import kotlinx.io.Source
import sk.ainet.clean.domain.port.InferenceModule
import sk.ai.net.nn.Module

/**
 * MLP adapter that wraps a sk.ai.net.nn.Module and exposes the InferenceModule port (PRD §8).
 * Mirrors CNN adapter but uses the MLP factory.
 */
class MlpInferenceModuleAdapter(
    private val loadFn: (weights: ByteArray) -> Unit,
    private val inferFn: (image: GrayScale28To28Image) -> Int,
) : InferenceModule {

    override fun load(weights: ByteArray) = loadFn(weights)

    override fun infer(image: GrayScale28To28Image): Int = inferFn(image)

    companion object {
        /** Create an adapter from an existing Module instance. */
        fun fromModule(module: Module): MlpInferenceModuleAdapter {
            return MlpInferenceModuleAdapter(
                loadFn = { bytes ->
                    val src: Source = Buffer().apply { write(bytes) }
                    loadModelWeights(module, src)
                },
                inferFn = { image -> classifyImage(module, image) }
            )
        }

        /** Create an adapter with a freshly built MLP Module. */
        fun create(): MlpInferenceModuleAdapter = fromModule(createMLP())
    }
}
