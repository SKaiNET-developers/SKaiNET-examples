package sk.ainet.clean.domain.model

/**
 * Identifier for a model variant used by the demo.
 *
 * Follows PRD §3 and §12. The type is extensible: add new constants in [companion object]
 * or construct dynamically if needed (e.g., remote-updated models).
 */
data class ModelId(val value: String) {
    companion object {
        /** CNN-based MNIST classifier identifier */
        val CNN_MNIST = ModelId("cnn-mnist")

        /** MLP-based MNIST classifier identifier */
        val MLP_MNIST = ModelId("mlp-mnist")
    }
}

/**
 * Optional model specification placeholder (PRD §3, §12).
 * Extend as needed when strategies require more detail than [ModelId].
 */
data class ModelSpec(
    val id: ModelId,
    val inputWidth: Int = 28,
    val inputHeight: Int = 28,
    val channels: Int = 1,
)
