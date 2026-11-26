package sk.ainet.apps.kllama.chat.qwen

import kotlinx.io.Buffer
import sk.ainet.context.ExecutionContext
import sk.ainet.io.ByteArrayRandomAccessSource
import sk.ainet.io.gguf.StreamingGGUFReader
import sk.ainet.io.gguf.StreamingGgufParametersLoader
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

/**
 * True when every tensor in the GGUF is a type the packed loader serves, so
 * the weights can stay in their stored block encoding. A single tensor of
 * another type (Q4_1, for example) makes the packed loader reject the file.
 */
fun ggufLoadsPacked(bytes: ByteArray): Boolean {
    val reader = StreamingGGUFReader.open(ByteArrayRandomAccessSource(bytes))
    return reader.tensors.all { it.tensorType in StreamingGgufParametersLoader.SUPPORTED_TENSOR_TYPES }
}

/**
 * The BOS token id from the GGUF metadata, or 1 when the file records none —
 * the same default the decoder weight loader applies. Qwen GGUFs record none
 * and Qwen's tokenizer reports -1, which is not a usable token id.
 */
fun ggufBosTokenId(bytes: ByteArray): Int {
    val reader = StreamingGGUFReader.open(ByteArrayRandomAccessSource(bytes))
    val id = when (val raw = reader.fields["tokenizer.ggml.bos_token_id"]) {
        is Number -> raw.toInt()
        is UInt -> raw.toInt()
        is ULong -> raw.toInt()
        else -> -1
    }
    return if (id >= 0) id else 1
}

/**
 * Default loader: keeps quantized tensors packed and reads them in place from
 * [bytes]. The matmuls run on the packed bytes and the working set stays
 * close to the file size. Works on every Kotlin target.
 */
suspend fun loadQwenPacked(ctx: ExecutionContext, bytes: ByteArray): Module<FP32, Float> =
    QwenNetworkLoader
        .fromGguf(randomAccessProvider = { ByteArrayRandomAccessSource(bytes) })
        .load<FP32, Float>(ctx)

/**
 * Compatibility escape hatch: dequantizes every tensor to FP32 at load. It
 * needs several times the memory of [loadQwenPacked] and is much slower, so
 * it is only for a GGUF that fails [ggufLoadsPacked]. Never use it to recover
 * from an out-of-memory error — it allocates more, not less.
 */
suspend fun loadQwenDequantizedToFp32(ctx: ExecutionContext, bytes: ByteArray): Module<FP32, Float> =
    QwenNetworkLoader
        .fromGguf(sourceProvider = { Buffer().apply { write(bytes) } })
        .load<FP32, Float>(ctx)

/** Loads packed when the file allows it, otherwise through the FP32 escape hatch. */
suspend fun loadQwen(ctx: ExecutionContext, bytes: ByteArray): Module<FP32, Float> =
    if (ggufLoadsPacked(bytes)) loadQwenPacked(ctx, bytes) else loadQwenDequantizedToFp32(ctx, bytes)
