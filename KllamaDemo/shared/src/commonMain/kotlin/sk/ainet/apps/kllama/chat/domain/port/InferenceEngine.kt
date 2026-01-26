package sk.ainet.apps.kllama.chat.domain.port

import kotlinx.coroutines.flow.Flow
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics

/**
 * Token generated during streaming inference.
 */
data class GeneratedToken(
    val token: String,
    val tokenId: Int,
    val statistics: InferenceStatistics
)

/**
 * Port interface for the inference engine.
 * Handles token generation from loaded models.
 */
interface InferenceEngine {
    /**
     * Generate tokens for the given chat session.
     * Emits tokens one at a time as they are generated.
     *
     * @param session The chat session containing conversation history
     * @param config Inference configuration parameters
     * @return Flow of generated tokens with statistics
     */
    fun generate(
        session: ChatSession,
        config: InferenceConfig = InferenceConfig()
    ): Flow<GeneratedToken>

    /**
     * Stop any ongoing generation.
     */
    fun stopGeneration()

    /**
     * Tokenize input text into token IDs.
     *
     * @param text Text to tokenize
     * @return List of token IDs
     */
    fun tokenize(text: String): List<Int>

    /**
     * Decode token IDs back to text.
     *
     * @param tokenIds Token IDs to decode
     * @return Decoded text
     */
    fun decode(tokenIds: List<Int>): String

    /**
     * Check if the engine is ready for inference.
     */
    val isReady: Boolean
}
