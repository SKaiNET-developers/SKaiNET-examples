package sk.ainet.samples.emoji.engine

/**
 * The MongoDB LEAF retrieval encoder (`mdbr-leaf-ir`, Apache-2.0): a 6-layer BERT with hidden
 * size 384, mean pooling and a `2_Dense` projection to 768 dimensions. Files are addressed by
 * their path inside the Hugging Face repository, which is also the on-disk layout everywhere.
 */
object LeafFiles {
    const val HF_REPO = "MongoDB/mdbr-leaf-ir"
    const val HF_REVISION = "4262131b32c3182bd06e67e92ae69d7bd66e0c5c"

    const val MODEL = "model.safetensors"
    const val CONFIG = "config.json"
    const val TOKENIZER = "tokenizer.json"
    const val TOKENIZER_CONFIG = "tokenizer_config.json"
    const val DENSE_MODEL = "2_Dense/model.safetensors"
    const val DENSE_CONFIG = "2_Dense/config.json"
    const val POOLING_CONFIG = "1_Pooling/config.json"

    val ALL: List<String> = listOf(MODEL, CONFIG, TOKENIZER, TOKENIZER_CONFIG, DENSE_MODEL, DENSE_CONFIG, POOLING_CONFIG)

    /** Sizes at [HF_REVISION], for progress reporting and truncation checks. */
    val SIZES: Map<String, Long> = mapOf(
        MODEL to 90_272_656L,
        CONFIG to 829L,
        TOKENIZER to 711_661L,
        TOKENIZER_CONFIG to 1_529L,
        DENSE_MODEL to 1_182_880L,
        DENSE_CONFIG to 137L,
        POOLING_CONFIG to 321L,
    )

    fun url(file: String): String = "https://huggingface.co/$HF_REPO/resolve/$HF_REVISION/$file"
}

/** Where the checkpoint files are: a directory (desktop, Android) or already-read bytes (web). */
sealed interface LeafSource {
    data class Directory(val path: String) : LeafSource

    class Bytes(
        val model: ByteArray,
        val denseModel: ByteArray,
        val configJson: String,
        val denseConfigJson: String,
        val poolingConfigJson: String,
        val tokenizerJson: String,
        val tokenizerConfigJson: String?,
    ) : LeafSource
}
