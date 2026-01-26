package sk.ainet.apps.kllama.chat.inference

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.GeneratedToken
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * Inference engine for LLM text generation.
 *
 * This implementation provides the framework for streaming token generation.
 * The actual neural network forward pass is delegated to platform-specific backends.
 */
class LlamaInferenceEngine(
    private val model: LoadedModel?,
    private val tokenizer: Tokenizer = SimpleByteTokenizer(),
    private val backend: InferenceBackend? = null
) : InferenceEngine {

    @Volatile
    private var shouldStop = false

    private val statisticsCollector = TokenStatisticsCollector()

    override val isReady: Boolean
        get() = model != null && backend != null

    override fun generate(
        session: ChatSession,
        config: InferenceConfig
    ): Flow<GeneratedToken> = flow {
        shouldStop = false

        // Format the conversation into a prompt
        val prompt = ChatFormatter.formatChatML(session)
        val promptTokens = tokenizer.encode(prompt)

        statisticsCollector.start(promptTokens.size)

        // If no backend is available, use demo mode
        if (backend == null) {
            generateDemoTokens(config).collect { emit(it) }
            return@flow
        }

        // Initialize generation state
        var currentTokens = promptTokens.toMutableList()
        var generatedCount = 0

        while (generatedCount < config.maxNewTokens && !shouldStop && currentCoroutineContext().isActive) {
            // Forward pass through the model
            val logits = backend.forward(currentTokens)

            // Sample next token
            val nextToken = sampleToken(logits, config)

            // Check for EOS
            if (nextToken == tokenizer.eosToken) {
                break
            }

            // Decode and emit the token
            val tokenText = tokenizer.decodeToken(nextToken)
            statisticsCollector.recordToken()

            emit(GeneratedToken(
                token = tokenText,
                tokenId = nextToken,
                statistics = statisticsCollector.buildStatistics()
            ))

            // Update state for next iteration
            currentTokens.add(nextToken)
            generatedCount++

            // Yield to allow cancellation
            yield()
        }
    }

    /**
     * Generate demo tokens for testing without a real model.
     */
    private fun generateDemoTokens(config: InferenceConfig): Flow<GeneratedToken> = flow {
        val demoResponse = buildDemoResponse()

        for ((index, char) in demoResponse.withIndex()) {
            if (shouldStop || !currentCoroutineContext().isActive) break
            if (index >= config.maxNewTokens) break

            statisticsCollector.recordToken()

            emit(GeneratedToken(
                token = char.toString(),
                tokenId = char.code,
                statistics = statisticsCollector.buildStatistics()
            ))

            // Simulate generation delay
            kotlinx.coroutines.delay(20 + Random.nextLong(30))
        }
    }

    private fun buildDemoResponse(): String {
        val responses = listOf(
            "Hello! I'm a demo response from the local LLM. The model is loaded and ready for inference. " +
                    "In a real implementation, this would be actual generated text from the neural network. " +
                    "The streaming display shows how tokens would appear one by one during generation.",
            "I see you've loaded a model! This is a demonstration of the chat interface. " +
                    "The real inference engine would perform forward passes through the transformer layers " +
                    "and sample tokens based on the probability distribution.",
            "Thanks for trying the offline chat app! This placeholder text demonstrates the streaming UI. " +
                    "When connected to a real GGUF model with proper weights, you'll see actual AI-generated responses.",
            "This is a test response showing the token-by-token streaming capability. " +
                    "Notice how the text appears progressively, simulating real LLM inference behavior. " +
                    "The statistics panel shows tokens per second and other metrics."
        )
        return responses.random()
    }

    /**
     * Sample a token from logits using temperature and top-p/top-k sampling.
     */
    private fun sampleToken(logits: FloatArray, config: InferenceConfig): Int {
        // Apply temperature
        val scaled = if (config.temperature != 1f) {
            logits.map { it / config.temperature }.toFloatArray()
        } else {
            logits
        }

        // Convert to probabilities using softmax
        val maxLogit = scaled.max()
        val exps = scaled.map { kotlin.math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        val probs = exps.map { it / sumExps }

        // Top-k filtering
        val sortedIndices = probs.indices.sortedByDescending { probs[it] }
        val topK = sortedIndices.take(config.topK)

        // Top-p (nucleus) filtering
        var cumSum = 0f
        val nucleus = mutableListOf<Int>()
        for (idx in topK) {
            nucleus.add(idx)
            cumSum += probs[idx]
            if (cumSum >= config.topP) break
        }

        // Sample from nucleus
        val nucleusProbs = nucleus.map { probs[it] }
        val nucleusSum = nucleusProbs.sum()
        val normalizedProbs = nucleusProbs.map { it / nucleusSum }

        var r = Random.nextFloat()
        for ((i, idx) in nucleus.withIndex()) {
            r -= normalizedProbs[i]
            if (r <= 0) return idx
        }

        return nucleus.last()
    }

    override fun stopGeneration() {
        shouldStop = true
    }

    override fun tokenize(text: String): List<Int> = tokenizer.encode(text)

    override fun decode(tokenIds: List<Int>): String = tokenizer.decode(tokenIds)
}

/**
 * Backend interface for platform-specific neural network execution.
 */
interface InferenceBackend {
    /**
     * Perform a forward pass through the model.
     *
     * @param tokens Input token IDs
     * @return Logits for the next token prediction
     */
    fun forward(tokens: List<Int>): FloatArray

    /**
     * Reset the KV cache for a new generation.
     */
    fun resetCache()
}
