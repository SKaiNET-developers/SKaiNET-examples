package sk.ainet.lang.model

import kotlinx.io.Buffer
import kotlinx.io.Source
import sk.ainet.io.gguf.GGUFReader
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * Android implementation using reflection-based GGUF loader.
 * Same approach as JVM since Android supports reflection.
 */
actual fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray) {
    val source: Source = Buffer().apply { write(bytes) }
    val reader = GGUFReader(source)
    val tensorMap = reader.tensors.associateBy { it.name }

    module.trainableParameters().forEach { param ->
        val readerTensor = tensorMap[param.name]
        if (readerTensor != null) {
            val tensorData = param.value.data
            // Use reflection to access the underlying buffer array
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
