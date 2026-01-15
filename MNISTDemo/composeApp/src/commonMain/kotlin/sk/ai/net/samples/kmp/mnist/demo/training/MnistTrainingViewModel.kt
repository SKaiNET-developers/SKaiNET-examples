package sk.ai.net.samples.kmp.mnist.demo.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import sk.ainet.clean.domain.training.MnistTrainer
import kotlin.random.Random

data class MnistTrainingState(
    val epoch: Int = 0,
    val totalEpochs: Int = 10,
    val currentLoss: Float = 0f,
    val currentAccuracy: Float = 0f,
    val lossHistory: List<Float> = emptyList(),
    val accuracyHistory: List<Float> = emptyList(),
    val isTraining: Boolean = false,
    val isCompleted: Boolean = false,
    val statusMessage: String = "Ready to train"
)

class MnistTrainingViewModel : ViewModel() {
    private val _trainingState = MutableStateFlow(MnistTrainingState())
    val trainingState: StateFlow<MnistTrainingState> = _trainingState.asStateFlow()

    // Counter to notify UI of model updates
    private val _modelUpdateCounter = MutableStateFlow(0)
    val modelUpdateCounter: StateFlow<Int> = _modelUpdateCounter.asStateFlow()

    private val trainer = MnistTrainer()
    private var trainingJob: Job? = null

    val isTraining: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            _trainingState.collect { state ->
                value = state.isTraining
            }
        }
    }.asStateFlow()

    /**
     * Start training with synthetic data for demonstration.
     * In a real app, you would load actual MNIST data.
     */
    fun startTraining(epochs: Int = 10, batchSize: Int = 32, learningRate: Double = 0.01) {
        if (trainingState.value.isTraining) return

        trainingJob = viewModelScope.launch(Dispatchers.Default) {
            _trainingState.update {
                it.copy(
                    isTraining = true,
                    isCompleted = false,
                    lossHistory = emptyList(),
                    accuracyHistory = emptyList(),
                    epoch = 0,
                    currentLoss = 0f,
                    currentAccuracy = 0f,
                    totalEpochs = epochs,
                    statusMessage = "Generating training data..."
                )
            }

            // Generate synthetic training data for demonstration
            // In production, load real MNIST data from resources
            val (images, labels) = generateSyntheticMnistData(500)

            _trainingState.update {
                it.copy(statusMessage = "Training...")
            }

            trainer.train(
                images = images,
                labels = labels,
                epochs = epochs,
                batchSize = batchSize,
                lr = learningRate
            )
                .conflate()
                .collect { progress ->
                    _trainingState.update { state ->
                        state.copy(
                            epoch = progress.epoch,
                            currentLoss = progress.loss,
                            currentAccuracy = progress.accuracy,
                            lossHistory = state.lossHistory + progress.loss,
                            accuracyHistory = state.accuracyHistory + progress.accuracy,
                            isCompleted = progress.isCompleted,
                            isTraining = !progress.isCompleted,
                            statusMessage = if (progress.isCompleted) "Training completed!" else "Epoch ${progress.epoch}/${state.totalEpochs}"
                        )
                    }
                    _modelUpdateCounter.update { it + 1 }
                    yield() // Extra yield after state update
                }
        }
    }

    fun stopTraining() {
        trainingJob?.cancel()
        trainingJob = null
        _trainingState.value = _trainingState.value.copy(
            isTraining = false,
            statusMessage = "Training stopped"
        )
    }

    /**
     * Predict a digit from a flattened 28x28 image.
     */
    fun predict(image: FloatArray): Int {
        return trainer.predict(image)
    }

    /**
     * Generate synthetic MNIST-like data for demonstration purposes.
     * Each digit class has a distinct pattern.
     */
    private suspend fun generateSyntheticMnistData(numSamples: Int): Pair<List<FloatArray>, List<Int>> {
        val images = mutableListOf<FloatArray>()
        val labels = mutableListOf<Int>()
        val random = Random(42)

        repeat(numSamples) {
            val label = random.nextInt(10)
            val image = FloatArray(784)

            // Create a simple pattern based on the digit
            // This is a placeholder - real MNIST data would be loaded from resources
            when (label) {
                0 -> drawCircle(image, random)
                1 -> drawVerticalLine(image, random)
                2 -> drawHorizontalLines(image, random)
                3 -> drawThree(image, random)
                4 -> drawFour(image, random)
                5 -> drawFive(image, random)
                6 -> drawSix(image, random)
                7 -> drawSeven(image, random)
                8 -> drawEight(image, random)
                9 -> drawNine(image, random)
            }

            // Add some noise
            for (i in image.indices) {
                image[i] = (image[i] + random.nextFloat() * 0.1f).coerceIn(0f, 1f)
            }

            images.add(image)
            labels.add(label)

            // Yield every 50 samples to keep UI responsive during generation
            if (it % 50 == 0) {
                yield()
            }
        }

        return images to labels
    }

    // Simple pattern generators for synthetic data
    private fun setPixel(image: FloatArray, x: Int, y: Int, value: Float = 1f) {
        if (x in 0..27 && y in 0..27) {
            image[y * 28 + x] = value
        }
    }

    private fun drawCircle(image: FloatArray, random: Random) {
        val cx = 14 + random.nextInt(3) - 1
        val cy = 14 + random.nextInt(3) - 1
        for (angle in 0..360 step 10) {
            val rad = angle * kotlin.math.PI / 180
            val x = (cx + 8 * kotlin.math.cos(rad)).toInt()
            val y = (cy + 8 * kotlin.math.sin(rad)).toInt()
            setPixel(image, x, y)
            setPixel(image, x + 1, y)
            setPixel(image, x, y + 1)
        }
    }

    private fun drawVerticalLine(image: FloatArray, random: Random) {
        val x = 14 + random.nextInt(3) - 1
        for (y in 4..24) {
            setPixel(image, x, y)
            setPixel(image, x + 1, y)
        }
    }

    private fun drawHorizontalLines(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (x in 6..22) {
            setPixel(image, x, 6 + offset)
            setPixel(image, x, 14 + offset)
            setPixel(image, x, 22 + offset)
        }
    }

    private fun drawThree(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (x in 8..20) {
            setPixel(image, x, 6 + offset)
            setPixel(image, x, 14 + offset)
            setPixel(image, x, 22 + offset)
        }
        for (y in 6..22) {
            setPixel(image, 20 + offset, y)
        }
    }

    private fun drawFour(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (y in 4..14) setPixel(image, 8 + offset, y)
        for (x in 8..20) setPixel(image, x, 14 + offset)
        for (y in 4..24) setPixel(image, 18 + offset, y)
    }

    private fun drawFive(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (x in 8..20) setPixel(image, x, 6 + offset)
        for (y in 6..14) setPixel(image, 8 + offset, y)
        for (x in 8..20) setPixel(image, x, 14 + offset)
        for (y in 14..22) setPixel(image, 20 + offset, y)
        for (x in 8..20) setPixel(image, x, 22 + offset)
    }

    private fun drawSix(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (y in 6..22) setPixel(image, 8 + offset, y)
        for (x in 8..20) setPixel(image, x, 6 + offset)
        for (x in 8..20) setPixel(image, x, 14 + offset)
        for (x in 8..20) setPixel(image, x, 22 + offset)
        for (y in 14..22) setPixel(image, 20 + offset, y)
    }

    private fun drawSeven(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (x in 8..20) setPixel(image, x, 6 + offset)
        for (y in 6..24) setPixel(image, 20 + offset, y)
    }

    private fun drawEight(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (y in 6..22) {
            setPixel(image, 8 + offset, y)
            setPixel(image, 20 + offset, y)
        }
        for (x in 8..20) {
            setPixel(image, x, 6 + offset)
            setPixel(image, x, 14 + offset)
            setPixel(image, x, 22 + offset)
        }
    }

    private fun drawNine(image: FloatArray, random: Random) {
        val offset = random.nextInt(3) - 1
        for (y in 6..14) setPixel(image, 8 + offset, y)
        for (x in 8..20) setPixel(image, x, 6 + offset)
        for (x in 8..20) setPixel(image, x, 14 + offset)
        for (y in 6..22) setPixel(image, 20 + offset, y)
        for (x in 8..20) setPixel(image, x, 22 + offset)
    }
}
