package sk.ainet.apps.kllama.chat.qwen

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import sk.ainet.context.ExecutionContext
import sk.ainet.io.ByteArrayRandomAccessSource
import sk.ainet.io.RandomAccessSource
import sk.ainet.io.gguf.StreamingGGUFReader
import sk.ainet.io.gguf.StreamingGgufParametersLoader
import sk.ainet.io.openRandomAccessSource
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

/** Where the GGUF lives. Every loader below reads it through [open]. */
sealed interface QwenModelSource {
    fun open(): RandomAccessSource

    /** The whole file held in memory. The packed weights then stay on the heap. */
    class InMemory(val bytes: ByteArray) : QwenModelSource {
        override fun open(): RandomAccessSource = ByteArrayRandomAccessSource(bytes)
    }

    /**
     * A file on disk. The loader memory-maps the packed weights, so they are
     * paged in by the OS and never count against a managed-heap limit.
     */
    class OnDisk(val path: String) : QwenModelSource {
        override fun open(): RandomAccessSource =
            openRandomAccessSource(path) ?: error("Cannot open GGUF for random access: $path")
    }
}

/**
 * True when every tensor in the GGUF is a type the packed loader serves, so
 * the weights can stay in their stored block encoding. A single tensor of
 * another type (Q4_1, for example) makes the packed loader reject the file.
 */
fun ggufLoadsPacked(source: QwenModelSource): Boolean = source.open().use { ras ->
    StreamingGGUFReader.open(ras).tensors
        .all { it.tensorType in StreamingGgufParametersLoader.SUPPORTED_TENSOR_TYPES }
}

/**
 * The BOS token id from the GGUF metadata, or 1 when the file records none —
 * the same default the decoder weight loader applies. Qwen GGUFs record none
 * and Qwen's tokenizer reports -1, which is not a usable token id.
 */
fun ggufBosTokenId(source: QwenModelSource): Int = source.open().use { ras ->
    val id = when (val raw = StreamingGGUFReader.open(ras).fields["tokenizer.ggml.bos_token_id"]) {
        is Number -> raw.toInt()
        is UInt -> raw.toInt()
        is ULong -> raw.toInt()
        else -> -1
    }
    if (id >= 0) id else 1
}

/**
 * Default loader: keeps quantized tensors packed. The matmuls run on the
 * packed bytes and the working set stays close to the file size. Works on
 * every Kotlin target.
 */
suspend fun loadQwenPacked(ctx: ExecutionContext, source: QwenModelSource): Module<FP32, Float> =
    QwenNetworkLoader
        .fromGguf(randomAccessProvider = { source.open() })
        .load<FP32, Float>(ctx)

/**
 * Compatibility escape hatch: dequantizes every tensor to FP32 at load. It
 * needs several times the memory of [loadQwenPacked] and is much slower, so
 * it is only for a GGUF that fails [ggufLoadsPacked]. Never use it to recover
 * from an out-of-memory error — it allocates more, not less.
 */
suspend fun loadQwenDequantizedToFp32(ctx: ExecutionContext, source: QwenModelSource): Module<FP32, Float> =
    QwenNetworkLoader
        .fromGguf(sourceProvider = { source.sequential() })
        .load<FP32, Float>(ctx)

/** Loads packed when the file allows it, otherwise through the FP32 escape hatch. */
suspend fun loadQwen(ctx: ExecutionContext, source: QwenModelSource): Module<FP32, Float> =
    if (ggufLoadsPacked(source)) loadQwenPacked(ctx, source) else loadQwenDequantizedToFp32(ctx, source)

private fun QwenModelSource.sequential(): Source = when (this) {
    is QwenModelSource.InMemory -> Buffer().apply { write(bytes) }
    is QwenModelSource.OnDisk -> SequentialRead(open()).buffered()
}

/** Front-to-back reads over a [RandomAccessSource], so a file is never held in memory whole. */
private class SequentialRead(private val source: RandomAccessSource) : RawSource {
    private var position = 0L
    private val chunk = ByteArray(64 * 1024)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (position >= source.size) return -1
        val want = minOf(byteCount, chunk.size.toLong(), source.size - position).toInt()
        val read = source.readAt(position, chunk, 0, want)
        if (read <= 0) return -1
        sink.write(chunk, 0, read)
        position += read
        return read.toLong()
    }

    override fun close() = source.close()
}
