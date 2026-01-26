package sk.ainet.apps.kllama.chat.domain.model

/**
 * Supported model file formats.
 */
enum class ModelFormat {
    GGUF,
    SAFETENSORS,
    UNKNOWN
}

/**
 * Quantization type information.
 */
enum class QuantizationType {
    F32,
    F16,
    Q8_0,
    Q4_K,
    Q4_K_M,
    Q5_K,
    Q5_K_M,
    Q6_K,
    UNKNOWN
}

/**
 * Metadata about a loaded model.
 */
data class ModelMetadata(
    val name: String,
    val parameterCount: Long,
    val sizeBytes: Long,
    val quantization: QuantizationType = QuantizationType.UNKNOWN,
    val contextLength: Int = 2048,
    val vocabSize: Int = 32000,
    val hiddenSize: Int = 0,
    val numLayers: Int = 0,
    val numHeads: Int = 0
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000 -> "%.2f GB".format(sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000 -> "%.2f MB".format(sizeBytes / 1_000_000.0)
            sizeBytes >= 1_000 -> "%.2f KB".format(sizeBytes / 1_000.0)
            else -> "$sizeBytes B"
        }

    val formattedParamCount: String
        get() = when {
            parameterCount >= 1_000_000_000 -> "%.2fB".format(parameterCount / 1_000_000_000.0)
            parameterCount >= 1_000_000 -> "%.2fM".format(parameterCount / 1_000_000.0)
            parameterCount >= 1_000 -> "%.2fK".format(parameterCount / 1_000.0)
            else -> "$parameterCount"
        }
}

/**
 * Represents a fully loaded model ready for inference.
 */
data class LoadedModel(
    val metadata: ModelMetadata,
    val modelPath: String,
    val format: ModelFormat
)

/**
 * Configuration for inference operations.
 */
data class InferenceConfig(
    val maxContextLength: Int = 2048,
    val maxNewTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f
)

/**
 * Real-time statistics during inference.
 */
data class InferenceStatistics(
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val totalTimeMs: Long = 0,
    val promptTokens: Int = 0
) {
    val formattedTps: String
        get() = "%.2f tok/s".format(tokensPerSecond)
}
