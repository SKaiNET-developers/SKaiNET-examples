package sk.ainet.lang.model

import kotlinx.io.Buffer
import sk.ainet.io.gguf.GGMLQuantizationType
import sk.ainet.io.gguf.GGUFReader
import sk.ainet.io.gguf.export.GGUFWriter
import sk.ainet.io.gguf.export.GgufTensorEntry
import sk.ainet.io.gguf.export.GgufWriteRequest
import sk.ainet.lang.nn.Module
import sk.ainet.lang.tensor.data.FloatArrayTensorData
import sk.ainet.lang.types.FP32

/**
 * Common-code GGUF weight loader. Works on every Kotlin target (JVM, Android, JS, wasmJs)
 * because it relies only on `FloatArrayTensorData.buffer` — no JVM reflection.
 *
 * Tensor names in [bytes] are matched to [Module.trainableParameters] by exact name.
 */
fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray) {
    val reader = GGUFReader(Buffer().apply { write(bytes) })
    val tensorMap = reader.tensors.associateBy { it.name }

    module.trainableParameters().forEach { param ->
        val readerTensor = tensorMap[param.name] ?: return@forEach
        val data = param.value.data
        if (data is FloatArrayTensorData<*>) {
            val buf = data.buffer
            readerTensor.data.forEachIndexed { idx, value ->
                buf[idx] = (value as Number).toFloat()
            }
        }
    }
}

/**
 * Serializes every trainable parameter of [module] as an FP32 GGUF, the format [loadWeightsFromBytes]
 * reads: one tensor per parameter under the parameter's name, plus `general.architecture`.
 * The trainer uses it to hand a freshly trained model to the inference side through the weights
 * repository, exactly like the pretrained files shipped with the sample.
 */
fun exportWeightsToBytes(module: Module<FP32, Float>, architecture: String): ByteArray {
    val entries = module.trainableParameters().map { param ->
        GgufTensorEntry(
            ggufName = param.name,
            tensor = param.value,
            quantization = GGMLQuantizationType.F32,
            shape = param.value.shape.dimensions.toList(),
        )
    }
    val request = GgufWriteRequest(
        metadata = mapOf("general.architecture" to architecture),
        tensors = entries,
        tensorMap = entries.associate { it.ggufName to it.ggufName },
    )
    return GGUFWriter.writeToByteArray(request).second
}
