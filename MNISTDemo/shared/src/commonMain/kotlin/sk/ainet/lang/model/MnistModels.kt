package sk.ainet.lang.model

import kotlinx.io.Buffer
import kotlinx.io.Source
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.context.observers.LatencyExecutionObserver
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.nn.dsl.sequential
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.types.FP32

/**
 * Common utilities for MNIST models.
 */

private val baseCtx = DirectCpuExecutionContext()
val latencyObserver = LatencyExecutionObserver().also {
    baseCtx.registerObserver(it)
}

private val evalCtx = DefaultGraphExecutionContext(
    baseOps = baseCtx.ops,
    phase = Phase.EVAL
)

fun createMNISTMLP(): Module<FP32, Float> = sequential(evalCtx) {
    input(784)
    dense(500, "fc1") { weights { randn(std = 0.1f) } }
    activation { it.relu() }
    dense(10, "fc2") { weights { randn(std = 0.1f) } }
}

/**
 * Creates a CNN for MNIST classification matching the pretrained weights structure.
 * Architecture:
 * - Input: 1x28x28 (grayscale image, will be reshaped from 784 flat input)
 * - Conv1: 1→16 channels, 5x5 kernel, padding 2, stride 1 → ReLU → MaxPool 2x2 → 14x14x16
 * - Conv2: 16→32 channels, 5x5 kernel, padding 2, stride 1 → ReLU → MaxPool 2x2 → 7x7x32
 * - Flatten: 7*7*32 = 1568
 * - Output: 1568→10 (dense layer)
 */
fun createMNISTCNN(): Module<FP32, Float> = sequential(evalCtx) {
    // Conv block 1: 1→16 channels (grayscale input has 1 channel)
    conv2d("stage1.conv1") {
        inChannels = 1
        outChannels = 16
        kernelSize(5)
        stride(1)
        padding(2)
    }
    activation { it.relu() }
    maxPool2d(kernelSize = 2 to 2, stride = 2 to 2)

    // Conv block 2: 16→32 channels
    conv2d("stage2.conv2") {
        inChannels = 16
        outChannels = 32
        kernelSize(5)
        stride(1)
        padding(2)
    }
    activation { it.relu() }
    maxPool2d(kernelSize = 2 to 2, stride = 2 to 2)

    // Flatten: 7*7*32 = 1568
    flatten()

    // Output layer: 1568→10
    dense(10, "out")
}

/**
 * Classifies an image using an MLP model (expects 2D input: batch x features).
 */
fun classifyImage(module: Module<FP32, Float>, image: GrayScale28To28Image): Int {
    return classifyImageMLP(module, image)
}

/**
 * Classifies an image using an MLP model.
 * Input shape: (1, 784) - flattened 28x28 image.
 */
fun classifyImageMLP(module: Module<FP32, Float>, image: GrayScale28To28Image): Int {
    val data = FloatArray(784)
    for (y in 0 until 28) {
        for (x in 0 until 28) {
            data[y * 28 + x] = image.getPixel(x, y)
        }
    }

    val input = baseCtx.fromFloatArray<FP32, Float>(
        Shape(1, 784),
        FP32::class,
        data
    )

    val output = module.forward(input, evalCtx)
    return argmax(output)
}

/**
 * Classifies an image using a CNN model.
 * Input shape: (1, 1, 28, 28) - batch x channels x height x width.
 */
fun classifyImageCNN(module: Module<FP32, Float>, image: GrayScale28To28Image): Int {
    val data = FloatArray(784)
    for (y in 0 until 28) {
        for (x in 0 until 28) {
            data[y * 28 + x] = image.getPixel(x, y)
        }
    }

    // CNN expects 4D input: (batch, channels, height, width)
    val input = baseCtx.fromFloatArray<FP32, Float>(
        Shape(1, 1, 28, 28),
        FP32::class,
        data
    )

    val output = module.forward(input, evalCtx)
    return argmax(output)
}

private fun argmax(output: sk.ainet.lang.tensor.Tensor<FP32, Float>): Int {
    var maxIndex = 0
    var maxValue = Float.NEGATIVE_INFINITY
    for (j in 0 until 10) {
        val value = output.data[0, j]
        if (value > maxValue) {
            maxValue = value
            maxIndex = j
        }
    }
    return maxIndex
}
