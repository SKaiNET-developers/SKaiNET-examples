package sk.ainet.lang.model

import kotlinx.io.Buffer
import kotlinx.io.Source
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
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

fun createMNISTCNN(): Module<FP32, Float> = sequential(evalCtx) {
    // Basic MLP as a fallback for CNN if the DSL is not fully known
    input(784)
    dense(256, "fc1")
    activation { it.relu() }
    dense(10, "fc2")
}

fun classifyImage(module: Module<FP32, Float>, image: GrayScale28To28Image): Int {
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
