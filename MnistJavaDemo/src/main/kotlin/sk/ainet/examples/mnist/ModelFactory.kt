/**
 * Kotlin bridge for model creation and weight loading.
 *
 * The SKaiNET `sequential` DSL uses `inline reified` type parameters,
 * which cannot be called from Java. This single Kotlin file bridges
 * the gap, providing Java-callable factory methods.
 *
 * From Java, call via: `ModelFactoryKt.createMnistCnn()` and
 * `ModelFactoryKt.loadGgufWeights(module, bytes)`.
 */
@file:JvmName("ModelFactoryKt")
package sk.ainet.examples.mnist

import kotlinx.io.Buffer
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.context.Phase
import sk.ainet.io.gguf.GGUFReader
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.nn.dsl.sequential
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.types.FP32

private val baseCtx = DirectCpuExecutionContext()
private val evalCtx = DefaultGraphExecutionContext(
    baseOps = baseCtx.ops,
    phase = Phase.EVAL
)

/**
 * Returns the shared execution context for tensor operations.
 * Use this from Java for creating input tensors.
 */
fun getBaseContext(): DirectCpuExecutionContext = baseCtx

/**
 * Returns the shared graph execution context for model inference.
 * Use this from Java for forward pass calls.
 */
fun getEvalContext(): ExecutionContext = evalCtx

/**
 * Creates an input tensor from a float array with the given shape.
 * Bridges Kotlin's reified KClass requirement for Java callers.
 */
fun createInputTensor(shape: Shape, data: FloatArray): Tensor<FP32, Float> =
    baseCtx.fromFloatArray(shape, FP32::class, data)

/**
 * Creates an MNIST CNN model matching the architecture used by MNISTDemo.
 * Architecture: conv(1->16,5x5) -> relu -> pool -> conv(16->32,5x5) -> relu -> pool -> flatten -> dense(10)
 */
fun createMnistCnn(): Module<FP32, Float> = sequential(evalCtx) {
    conv2d("stage1.conv1") {
        inChannels = 1
        outChannels = 16
        kernelSize(5)
        stride(1)
        padding(2)
    }
    activation { it.relu() }
    maxPool2d(kernelSize = 2 to 2, stride = 2 to 2)

    conv2d("stage2.conv2") {
        inChannels = 16
        outChannels = 32
        kernelSize(5)
        stride(1)
        padding(2)
    }
    activation { it.relu() }
    maxPool2d(kernelSize = 2 to 2, stride = 2 to 2)

    flatten()
    dense(10, "out")
}

/**
 * Loads GGUF weights into a module using reflection-based parameter copying.
 */
fun loadGgufWeights(module: Module<FP32, Float>, modelBytes: ByteArray) {
    val source = Buffer().apply { write(modelBytes) }
    val reader = GGUFReader(source)
    val tensorMap = reader.tensors.associateBy { it.name }

    module.trainableParameters().forEach { param ->
        val readerTensor = tensorMap[param.name]
        if (readerTensor != null) {
            val tensorData = param.value.data
            val bufferField = tensorData::class.java.declaredFields.firstOrNull {
                it.type.isArray && it.type.componentType == Float::class.javaPrimitiveType
            }
            if (bufferField != null) {
                bufferField.isAccessible = true
                val array = bufferField.get(tensorData) as FloatArray
                readerTensor.data.forEachIndexed { idx, value ->
                    array[idx] = (value as Number).toFloat()
                }
            }
        }
    }
}
