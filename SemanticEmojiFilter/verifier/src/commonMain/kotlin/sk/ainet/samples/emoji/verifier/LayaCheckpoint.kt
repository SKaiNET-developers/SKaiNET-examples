package sk.ainet.samples.emoji.verifier

/**
 * The multilingual Laya checkpoint (mmBERT-base encoder, Apache-2.0, Convai Innovations) as
 * published on Hugging Face: a directory of five files, addressed by their path inside it.
 */
object LayaCheckpoint {
    const val HF_REPO = "convaiinnovations/laya"
    const val HF_REVISION = "7b928d828b7b0e022f929d9bd2e44165aa270148"
    const val HF_DIR = "multilingual"

    val FILES: List<String> = listOf(
        "rl_agent_config.json",
        "encoder/config.json",
        "tokenizer/tokenizer.json",
        "tokenizer/tokenizer_config.json",
        "model.safetensors",
    )

    val SIZES: Map<String, Long> = mapOf("model.safetensors" to 643_835_514L)

    fun url(file: String): String = "https://huggingface.co/$HF_REPO/resolve/$HF_REVISION/$HF_DIR/$file"
}
