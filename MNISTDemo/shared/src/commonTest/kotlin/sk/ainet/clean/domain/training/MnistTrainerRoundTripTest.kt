package sk.ainet.clean.domain.training

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.lang.model.classifyImage
import sk.ainet.lang.model.createMNISTMLP
import sk.ainet.lang.model.loadWeightsFromBytes
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.data.FloatArrayTensorData

/**
 * The training tab hands its weights to the inference side as GGUF bytes through the weights
 * repository. This pins the contract: the exported bytes are a GGUF the sample's loader reads,
 * every parameter survives the round trip bit for bit, and the inference MLP then predicts
 * exactly what the trainer's own model predicts. (Before, the export was a 1024-byte placeholder
 * and the first load after training failed.)
 */
class MnistTrainerRoundTripTest {

    private fun imageOf(pixels: FloatArray): GrayScale28To28Image {
        val image = GrayScale28To28Image()
        for (y in 0 until 28) for (x in 0 until 28) image.setPixel(x, y, pixels[y * 28 + x])
        return image
    }

    /** The flat storage the sample's loader writes into; both models keep FP32 arrays. */
    private fun values(tensor: Tensor<*, *>): FloatArray {
        val data = tensor.data
        check(data is FloatArrayTensorData<*>) { "expected FloatArrayTensorData, got ${data::class.simpleName}" }
        return data.buffer.copyOf()
    }

    @Test
    fun exported_weights_load_into_the_inference_mlp_and_predict_the_same() = runTest {
        val random = Random(7)
        val images = List(24) { FloatArray(784) { random.nextFloat() } }
        val labels = List(24) { random.nextInt(10) }

        val trainer = MnistTrainer()
        val progress = trainer.train(images, labels, epochs = 1, batchSize = 8, lr = 0.01).toList()
        assertEquals(1, progress.size)
        assertTrue(progress.single().isCompleted)

        val bytes = trainer.exportWeights()
        assertContentEquals("GGUF".encodeToByteArray(), bytes.copyOfRange(0, 4), "export must be a GGUF file")

        val inference = createMNISTMLP()
        loadWeightsFromBytes(inference, bytes)

        val trained = trainer.getModel().trainableParameters().associate { it.name to it.value }
        val loaded = inference.trainableParameters().associate { it.name to it.value }
        assertEquals(setOf("fc1.weight", "fc1.bias", "fc2.weight", "fc2.bias"), loaded.keys)
        for ((name, source) in trained) {
            val target = loaded.getValue(name)
            assertContentEquals(source.shape.dimensions, target.shape.dimensions, "$name shape")
            assertContentEquals(values(source), values(target), "$name values")
        }

        repeat(6) {
            val pixels = FloatArray(784) { random.nextFloat() }
            assertEquals(trainer.predict(pixels), classifyImage(inference, imageOf(pixels)), "prediction $it")
        }
    }
}
