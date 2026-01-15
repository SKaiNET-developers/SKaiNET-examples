package sk.ainet.clean.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.data.repository.ModelWeightsRepositoryImpl
import sk.ainet.clean.data.source.ModelWeightsDataSource
import sk.ainet.clean.data.source.ModelWeightsLocalDataSource
import sk.ainet.clean.domain.factory.DigitClassifierFactory
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.domain.port.ModelWeightsRepository
import kotlinx.coroutines.test.runTest

private class FakeResourceReader(private val map: Map<String, ByteArray>) : ResourceReader {
    override suspend fun read(path: String): ByteArray? = map[path]
}

private class TestInferenceModule(
    private val resultDigit: Int,
) : InferenceModule {
    var loadCalls: Int = 0
    var inferCalls: Int = 0
    var lastLoaded: ByteArray? = null
    override fun load(weights: ByteArray) {
        loadCalls += 1
        lastLoaded = weights
    }

    override fun infer(image: GrayScale28To28Image): Int {
        inferCalls += 1
        return resultDigit
    }
}

/** Test-local factory to verify selection wiring without depending on another module's impl. */
private class TestDigitClassifierFactory(
    private val repository: ModelWeightsRepository,
    private val cnnProvider: () -> TestInferenceModule,
    private val mlpProvider: () -> TestInferenceModule,
) : DigitClassifierFactory {
    override fun create(modelId: ModelId): DigitClassifier = when (modelId.value) {
        ModelId.CNN_MNIST.value -> TestDigitClassifier(cnnProvider(), repository)
        ModelId.MLP_MNIST.value -> TestDigitClassifier(mlpProvider(), repository)
        else -> error("Unsupported ModelId: ${modelId.value}")
    }
}

/** Minimal classifier used in tests to assert load-before-classify and weights application. */
private class TestDigitClassifier(
    private val inference: TestInferenceModule,
    private val repository: ModelWeightsRepository,
) : DigitClassifier {
    private var loaded = false
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

class FactoryAndLoadIntegrationTest {

    private fun makeRepository(bytesCnn: ByteArray, bytesMlp: ByteArray): ModelWeightsRepository {
        val pathResolver: (ModelId) -> String = { id ->
            when (id.value) {
                ModelId.CNN_MNIST.value -> "files/mnist_cnn.gguf"
                ModelId.MLP_MNIST.value -> "files/mnist_mlp.gguf"
                else -> error("Unsupported ${id.value}")
            }
        }
        val reader = FakeResourceReader(
            mapOf(
                "files/mnist_cnn.gguf" to bytesCnn,
                "files/mnist_mlp.gguf" to bytesMlp,
            )
        )
        val local: ModelWeightsDataSource = ModelWeightsLocalDataSource(reader, pathResolver)
        return ModelWeightsRepositoryImpl(cache = null, local = local, remote = null)
    }

    @Test
    fun factory_creates_correct_classifier_and_requires_load_before_classify() = runTest {
        val weightsCnn = byteArrayOf(1, 2, 3)
        val weightsMlp = byteArrayOf(9, 8, 7)
        val repo = makeRepository(weightsCnn, weightsMlp)

        val cnnModule = TestInferenceModule(resultDigit = 3)
        val mlpModule = TestInferenceModule(resultDigit = 8)
        val factory = TestDigitClassifierFactory(
            repository = repo,
            cnnProvider = { cnnModule },
            mlpProvider = { mlpModule },
        )

        // CNN path
        val cnnClassifier = factory.create(ModelId.CNN_MNIST)
        // Classify before load should fail
        assertFailsWith<IllegalStateException> {
            cnnClassifier.classify(GrayScale28To28Image())
        }
        // After load, should pass and weights should be applied to inference module
        cnnClassifier.loadModel(ModelId.CNN_MNIST)
        assertEquals(1, cnnModule.loadCalls, "CNN module must be loaded exactly once")
        assertTrue(cnnModule.lastLoaded contentEquals weightsCnn, "CNN weights applied to module")
        val cnnResult = cnnClassifier.classify(GrayScale28To28Image())
        assertEquals(3, cnnResult, "CNN infer result should come from CNN module")
        assertEquals(1, cnnModule.inferCalls)

        // MLP path
        val mlpClassifier = factory.create(ModelId.MLP_MNIST)
        assertFailsWith<IllegalStateException> {
            mlpClassifier.classify(GrayScale28To28Image())
        }
        mlpClassifier.loadModel(ModelId.MLP_MNIST)
        assertEquals(1, mlpModule.loadCalls, "MLP module must be loaded exactly once")
        assertTrue(mlpModule.lastLoaded contentEquals weightsMlp, "MLP weights applied to module")
        val mlpResult = mlpClassifier.classify(GrayScale28To28Image())
        assertEquals(8, mlpResult, "MLP infer result should come from MLP module")
        assertEquals(1, mlpModule.inferCalls)
    }
}
