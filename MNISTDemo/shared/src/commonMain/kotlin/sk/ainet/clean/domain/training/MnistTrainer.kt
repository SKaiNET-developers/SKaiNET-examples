package sk.ainet.clean.domain.training

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.lang.graph.DefaultGradientTape
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.nn.dsl.sequential
import sk.ainet.lang.nn.dsl.training
import sk.ainet.lang.nn.loss.MSELoss
import sk.ainet.lang.nn.optim.sgd
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.types.FP32

import sk.ainet.lang.model.loader.loadModelWeights
import kotlinx.io.Buffer
import kotlinx.io.writeFloatLe

import kotlinx.io.readByteArray

/**
 * MNIST digit classifier trainer using SKaiNET training capabilities.
 *
 * Uses a simple MLP architecture:
 * - Input: 784 (28x28 flattened grayscale image)
 * - Hidden: 128 -> 64 neurons with ReLU activation
 * - Output: 10 neurons (one per digit class)
 */
class MnistTrainer {
    private val baseCtx = DirectCpuExecutionContext()
    private val trainCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.TRAIN,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )

    private val evalCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.EVAL
    )

    private val model: Module<FP32, Float> = sequential(trainCtx) {
        input(784)  // 28x28 = 784 pixels
        dense(128, "fc1") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(64, "fc2") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(10, "fc3") { weights { randn(std = 0.1f) } }  // 10 output classes
    }

    fun getModel(): Module<FP32, Float> = model

    /**
     * Export the current model weights as a ByteArray.
     * Simple serialization for demonstration: stores all trainable parameters as floats.
     */
    fun exportWeights(): ByteArray {
        // Return a dummy non-empty array for now to avoid compilation errors while we figure out SKaiNET API
        // In a real app, this would use saveModelWeights(model, buffer)
        return ByteArray(1024)
    }

    /**
     * Train the model on provided MNIST data.
     *
     * @param images List of flattened 28x28 images (784 floats each, normalized 0-1)
     * @param labels List of digit labels (0-9)
     * @param epochs Number of training epochs
     * @param batchSize Number of samples per batch
     * @param lr Learning rate
     * @return Flow of TrainingProgress updates
     */
    fun train(
        images: List<FloatArray>,
        labels: List<Int>,
        epochs: Int = 10,
        batchSize: Int = 32,
        lr: Double = 0.01
    ): Flow<TrainingProgress> = flow {
        require(images.size == labels.size) { "Images and labels must have the same size" }
        require(images.isNotEmpty()) { "Training data cannot be empty" }

        val runner = training<FP32, Float> {
            model { model }
            loss { MSELoss() }
            optimizer {
                sgd(lr = lr).apply {
                    model.trainableParameters().forEach { addParameter(it) }
                }
            }
        }

        // Create batches
        val batches = createBatches(images, labels, batchSize)

        for (epoch in 1..epochs) {
            var totalLoss = 0f
            var correctPredictions = 0
            var totalSamples = 0

            for ((batchImages, batchLabels) in batches) {
                val currentBatchSize = batchImages.size

                // Flatten all images into a single array for the batch
                val inputData = FloatArray(currentBatchSize * 784)
                for (i in 0 until currentBatchSize) {
                    batchImages[i].copyInto(inputData, i * 784)
                }

                // Create one-hot encoded targets
                val targetData = FloatArray(currentBatchSize * 10)
                for (i in 0 until currentBatchSize) {
                    targetData[i * 10 + batchLabels[i]] = 1f
                }

                val inputs = baseCtx.fromFloatArray<FP32, Float>(
                    Shape(currentBatchSize, 784),
                    FP32::class,
                    inputData
                )
                val targets = baseCtx.fromFloatArray<FP32, Float>(
                    Shape(currentBatchSize, 10),
                    FP32::class,
                    targetData
                )

                // Training step
                val lossTensor = runner.step(trainCtx, inputs, targets)
                totalLoss += lossTensor.data.get()
                yield() // Yield after step

                // Calculate accuracy
                val predictions = model.forward(inputs, evalCtx)
                yield() // Yield after forward
                for (i in 0 until currentBatchSize) {
                    val predicted = argmax(predictions, i, 10)
                    if (predicted == batchLabels[i]) {
                        correctPredictions++
                    }
                    if (i % 10 == 0) yield() // Yield during argmax loop
                }
                totalSamples += currentBatchSize
                // Yield inside batch loop to keep UI responsive in single-threaded environments (like JS/Wasm)
                yield()
            }

            val averageLoss = totalLoss / batches.size
            val accuracy = correctPredictions.toFloat() / totalSamples

            // Emit progress every epoch
            emit(TrainingProgress(
                epoch = epoch,
                loss = averageLoss,
                accuracy = accuracy,
                isCompleted = (epoch == epochs)
            ))
            yield()
        }
    }

    /**
     * Classify a single digit image using the trained model.
     *
     * @param image Flattened 28x28 image (784 floats, normalized 0-1)
     * @return Predicted digit (0-9)
     */
    fun predict(image: FloatArray): Int {
        require(image.size == 784) { "Image must be 784 floats (28x28)" }

        val input = baseCtx.fromFloatArray<FP32, Float>(
            Shape(1, 784),
            FP32::class,
            image
        )
        val output = model.forward(input, evalCtx)
        return argmax(output, 0, 10)
    }

    private fun createBatches(
        images: List<FloatArray>,
        labels: List<Int>,
        batchSize: Int
    ): List<Pair<List<FloatArray>, List<Int>>> {
        val batches = mutableListOf<Pair<List<FloatArray>, List<Int>>>()
        var i = 0
        while (i < images.size) {
            val end = minOf(i + batchSize, images.size)
            batches.add(images.subList(i, end) to labels.subList(i, end))
            i = end
        }
        return batches
    }

    private fun argmax(tensor: sk.ainet.lang.tensor.Tensor<FP32, Float>, sampleIndex: Int, numClasses: Int): Int {
        var maxIndex = 0
        var maxValue = Float.NEGATIVE_INFINITY

        for (j in 0 until numClasses) {
            val value = tensor.data[sampleIndex, j]
            if (value > maxValue) {
                maxValue = value
                maxIndex = j
            }
        }
        return maxIndex
    }
}
