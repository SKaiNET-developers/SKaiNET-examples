package sk.ainet.samples.emoji.engine

import kotlin.time.TimeSource
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.frontend.text.BatchTokenizer
import sk.ainet.frontend.tokenizer.TextFrontEnds
import sk.ainet.io.RandomAccessSource
import sk.ainet.io.openRandomAccessSource
import sk.ainet.io.safetensors.SafeTensorsParametersLoader
import sk.ainet.lang.types.FP32
import sk.ainet.models.bert.BertConfigParser
import sk.ainet.models.bert.BertEncoderRuntime
import sk.ainet.models.bert.BertExecutionMode
import sk.ainet.models.bert.BertNetworkLoader
import sk.ainet.models.bert.PooledExecutionContext
import sk.ainet.models.bert.createBertEncoderRuntime
import sk.ainet.samples.emoji.platform.readTextFile

/** Sentence embeddings: one L2-normalised vector per text, so cosine is a dot product. */
interface EmbeddingEngine {
    val dimensions: Int

    /** Embeds a catalog text (a "document" in retrieval terms). */
    fun embed(text: String): FloatArray

    /**
     * Embeds a search query. LEAF is an asymmetric retrieval encoder: queries carry the prompt
     * its model card prescribes, documents do not; a symmetric engine may ignore the distinction.
     */
    fun embedQuery(text: String): FloatArray = embed(QUERY_PROMPT + text)

    companion object {
        /** The `query` prompt of `MongoDB/mdbr-leaf-ir` (sentence-transformers `prompt_name="query"`). */
        const val QUERY_PROMPT = "Represent this sentence for searching relevant passages: "
    }
}

/** The execution context every target uses; the kernel backend is picked by what is on the classpath. */
fun defaultExecutionContext(): ExecutionContext = PooledExecutionContext(DirectCpuExecutionContext())

/** LEAF on the SKaiNET-transformers BERT runtime: WordPiece tokenizer, encoder, mean pooling, 2_Dense, L2. */
class LeafEmbeddingEngine(
    private val runtime: BertEncoderRuntime<FP32>,
    private val tokenizer: BatchTokenizer,
    val loadMillis: Long,
) : EmbeddingEngine {
    override val dimensions: Int get() = runtime.dimensions

    override fun embed(text: String): FloatArray {
        val encoding = tokenizer.encodeWithMetadata(text)
        return runtime.encode(encoding.ids, encoding.attentionMask)
    }

    companion object {
        /**
         * Builds the engine from a [LeafSource]. The weights stream through the SafeTensors reader
         * from disk or from the in-memory bytes; the projection head comes from the second file.
         */
        suspend fun load(
            source: LeafSource,
            ctx: ExecutionContext = defaultExecutionContext(),
            mode: BertExecutionMode = BertExecutionMode.DIRECT,
            onProgress: (String) -> Unit = {},
        ): LeafEmbeddingEngine {
            val started = TimeSource.Monotonic.markNow()
            val files = when (source) {
                is LeafSource.Directory -> DirectoryFiles(source.path)
                is LeafSource.Bytes -> BytesFiles(source)
            }
            onProgress("Reading model configuration…")
            val config = BertConfigParser.parse(files.text(LeafFiles.CONFIG), files.text(LeafFiles.DENSE_CONFIG))
            val pooling = BertConfigParser.parsePooling(files.text(LeafFiles.POOLING_CONFIG))
            // LEAF's tokenizer.json is WordPiece; the engine returns the BERT-class tokenizer with masks.
            val tokenizer = TextFrontEnds.fromTokenizerJson(files.text(LeafFiles.TOKENIZER), files.textOrNull(LeafFiles.TOKENIZER_CONFIG)) as? BatchTokenizer
                ?: error("tokenizer.json is not a WordPiece/BERT-class tokenizer")

            onProgress("Loading encoder weights…")
            val loaders = listOf(
                SafeTensorsParametersLoader(sourceProvider = { files.source(LeafFiles.MODEL) }),
                SafeTensorsParametersLoader(sourceProvider = { files.source(LeafFiles.DENSE_MODEL) }),
            )
            val tensors = BertNetworkLoader.loadWeightTensors(loaders, ctx, FP32::class)
            onProgress("Building encoder…")
            val runtime = createBertEncoderRuntime(config, tensors, ctx, mode = mode, pooling = pooling)
            return LeafEmbeddingEngine(runtime, tokenizer, started.elapsedNow().inWholeMilliseconds)
        }
    }
}

private interface LeafFileAccess {
    fun text(file: String): String
    fun textOrNull(file: String): String?
    fun source(file: String): RandomAccessSource
}

private class DirectoryFiles(private val dir: String) : LeafFileAccess {
    private fun path(file: String) = "$dir/$file"
    override fun text(file: String): String = readTextFile(path(file))
    override fun textOrNull(file: String): String? = runCatching { readTextFile(path(file)) }.getOrNull()
    override fun source(file: String): RandomAccessSource =
        openRandomAccessSource(path(file)) ?: error("no random-access file source on this platform for ${path(file)}")
}

private class BytesFiles(private val bytes: LeafSource.Bytes) : LeafFileAccess {
    override fun text(file: String): String = when (file) {
        LeafFiles.CONFIG -> bytes.configJson
        LeafFiles.DENSE_CONFIG -> bytes.denseConfigJson
        LeafFiles.POOLING_CONFIG -> bytes.poolingConfigJson
        LeafFiles.TOKENIZER -> bytes.tokenizerJson
        LeafFiles.TOKENIZER_CONFIG -> bytes.tokenizerConfigJson ?: error("no tokenizer_config.json")
        else -> error("not a text file: $file")
    }

    override fun textOrNull(file: String): String? = if (file == LeafFiles.TOKENIZER_CONFIG) bytes.tokenizerConfigJson else text(file)

    override fun source(file: String): RandomAccessSource = when (file) {
        LeafFiles.MODEL -> ByteArrayRandomAccessSource(bytes.model)
        LeafFiles.DENSE_MODEL -> ByteArrayRandomAccessSource(bytes.denseModel)
        else -> error("not a tensor file: $file")
    }
}
