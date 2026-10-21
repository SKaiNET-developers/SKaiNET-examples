package sk.ainet.samples.layamoe.model

/** One file of a checkpoint: where it lives in the local model directory, where to fetch it, and how big it is. */
data class CheckpointFile(
    /** Path inside the example's model directory, e.g. `base/encoder/config.json`. */
    val relative: String,
    val url: String,
    /** Known size in bytes (truncated downloads are retried), or null when it does not matter. */
    val sizeBytes: Long?,
) {
    val name: String get() = relative.substringAfterLast('/')
}

/**
 * The checkpoints behind the mixture, pinned by Hugging Face revision: the base Laya model
 * (`laya-typed-decisions`, ModernBERT-large encoder + the original head, Apache-2.0, ConvAI
 * Innovations) and the two expert heads of layaMOE (Apache-2.0, vishalmysore; unofficial
 * derivatives of that head, fine-tuned on synthetic domain data). The base is the same five files
 * Laya publishes; an expert is `expert.json` (name, domains, calibration) + `expert.safetensors`
 * (type embedding, two head blocks, scorer — 26.5M parameters, FP32).
 */
object LayaMoeCheckpoints {
    const val BASE_REPO = "convaiinnovations/laya-typed-decisions"
    const val BASE_REVISION = "e929ae5cf69bc34259cd2f95c9e91145b818b1f0"
    const val EXPERTS_REPO = "VishalMysore/layaMOE"
    const val EXPERTS_REVISION = "07b7776d4c628ce828bf03ce231268caa21cf244"

    const val BASE_WEIGHTS = "model.safetensors"
    private const val BASE_WEIGHT_BYTES = 842_609_220L
    private const val EXPERT_WEIGHT_BYTES = 106_051_868L

    /** The expert names, in the order the mixture lists them after `general`. */
    val EXPERT_NAMES: List<String> = listOf("safety", "customer_ops")

    val baseAgentConfig: CheckpointFile = base("rl_agent_config.json")
    val baseEncoderConfig: CheckpointFile = base("encoder/config.json")
    val baseTokenizer: CheckpointFile = base("tokenizer/tokenizer.json")
    val baseTokenizerConfig: CheckpointFile = base("tokenizer/tokenizer_config.json")
    val baseWeights: CheckpointFile = base(BASE_WEIGHTS, BASE_WEIGHT_BYTES)

    val baseFiles: List<CheckpointFile> = listOf(baseAgentConfig, baseEncoderConfig, baseTokenizer, baseTokenizerConfig, baseWeights)

    fun expertMeta(name: String): CheckpointFile = expert(name, "expert.json", null)
    fun expertWeights(name: String): CheckpointFile = expert(name, "expert.safetensors", EXPERT_WEIGHT_BYTES)

    val expertFiles: List<CheckpointFile> = EXPERT_NAMES.flatMap { listOf(expertMeta(it), expertWeights(it)) }

    /** Everything the mixture loads, base first. */
    val allFiles: List<CheckpointFile> = baseFiles + expertFiles

    /** Bytes of the weight files — what a first run downloads, give or take the small JSON files. */
    val totalWeightBytes: Long = BASE_WEIGHT_BYTES + EXPERT_NAMES.size * EXPERT_WEIGHT_BYTES

    fun hfUrl(repo: String, revision: String, path: String): String = "https://huggingface.co/$repo/resolve/$revision/$path"

    private fun base(path: String, size: Long? = null) = CheckpointFile("base/$path", hfUrl(BASE_REPO, BASE_REVISION, path), size)
    private fun expert(name: String, file: String, size: Long?) = CheckpointFile("experts/$name/$file", hfUrl(EXPERTS_REPO, EXPERTS_REVISION, "$name/$file"), size)
}
