package sk.ainet.apps.kllama.chat.inference

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import sk.ainet.apps.kllama.chat.runtime.LlamaRuntime
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.GeneratedToken
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.lang.tensor.*
import sk.ainet.lang.types.DType
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * Inference engine for LLM text generation using SKaiNET's LlamaRuntime.
 *
 * When a LlamaRuntime is provided, this engine performs actual neural network inference.
 * Otherwise, it falls back to demo mode for testing the UI.
 */
class LlamaInferenceEngine(
    private val model: LoadedModel?,
    private val tokenizer: Tokenizer = SimpleByteTokenizer(),
    private val runtime: LlamaRuntime? = null
) : InferenceEngine {

    @Volatile
    private var shouldStop = false

    private val statisticsCollector = TokenStatisticsCollector()

    override val isReady: Boolean
        get() = model != null && runtime != null

    override fun generate(
        session: ChatSession,
        config: InferenceConfig
    ): Flow<GeneratedToken> = flow {
        shouldStop = false

        // If no runtime is available, use demo mode
        if (runtime == null) {
            println("No LlamaRuntime available, using demo mode")
            generateDemoTokens(config).collect { emit(it) }
            return@flow
        }

        // Format the conversation into a prompt
        val prompt = ChatFormatter.formatChatML(session)

        // Tokenize the prompt (simple byte-level for now)
        // TODO: Use proper BPE tokenizer from model vocabulary
        val promptTokens = tokenizer.encode(prompt).toIntArray()

        // Use forward()-based generation for proper streaming
        generateWithRuntime(promptTokens, config).collect { emit(it) }
    }

    /**
     * Generate tokens using LlamaRuntime's forward() method for streaming.
     */
    private fun generateWithRuntime(
        promptTokens: IntArray,
        config: InferenceConfig
    ): Flow<GeneratedToken> = flow {
        if (runtime == null) return@flow

        runtime.reset()
        statisticsCollector.start(promptTokens.size)

        // Process prompt tokens (prefill)
        var lastLogits: Any? = null
        for (tokenId in promptTokens) {
            if (shouldStop || !currentCoroutineContext().isActive) return@flow
            lastLogits = runtime.forward(tokenId)
        }

        // Generate new tokens
        var generatedCount = 0
        var nextToken = sampleFromLogits(lastLogits, config)

        while (generatedCount < config.maxNewTokens && !shouldStop && currentCoroutineContext().isActive) {
            // Check for EOS token (commonly 2 for Llama)
            if (nextToken == 2) break

            val tokenText = tokenizer.decodeToken(nextToken)
            statisticsCollector.recordToken()

            emit(GeneratedToken(
                token = tokenText,
                tokenId = nextToken,
                statistics = statisticsCollector.buildStatistics()
            ))

            // Forward pass for next token
            lastLogits = runtime.forward(nextToken)
            nextToken = sampleFromLogits(lastLogits, config)
            generatedCount++
        }
    }

    /**
     * Sample a token from the logits tensor.
     */
    @Suppress("UNCHECKED_CAST")
    private fun sampleFromLogits(logits: Any?, config: InferenceConfig): Int {
        if (logits == null) return 0

        // Extract float array from tensor
        val logitsArray = try {
            val tensor = logits as? sk.ainet.lang.tensor.Tensor<out sk.ainet.lang.types.DType, *>
            if (tensor != null) {
                val data = tensor.data
                FloatArray(tensor.volume) { idx ->
                    (data[idx] as Number).toFloat()
                }
            } else {
                return 0
            }
        } catch (e: Exception) {
            println("Error extracting logits: ${e.message}")
            return 0
        }

        return sampleToken(logitsArray, config)
    }

    /**
     * Generate demo tokens for testing without a real model.
     */
    private fun generateDemoTokens(config: InferenceConfig): Flow<GeneratedToken> = flow {
        val demoResponse = buildDemoResponse()

        statisticsCollector.start(0)

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
        if (logits.isEmpty()) return 0

        // Apply temperature
        val scaled = if (config.temperature != 1f && config.temperature > 0f) {
            logits.map { it / config.temperature }.toFloatArray()
        } else {
            logits
        }

        // Convert to probabilities using softmax
        val maxLogit = scaled.maxOrNull() ?: 0f
        val exps = scaled.map { kotlin.math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        if (sumExps == 0f) return 0
        val probs = exps.map { it / sumExps }

        // Top-k filtering
        val sortedIndices = probs.indices.sortedByDescending { probs[it] }
        val topK = sortedIndices.take(minOf(config.topK, probs.size))

        // Top-p (nucleus) filtering
        var cumSum = 0f
        val nucleus = mutableListOf<Int>()
        for (idx in topK) {
            nucleus.add(idx)
            cumSum += probs[idx]
            if (cumSum >= config.topP) break
        }

        if (nucleus.isEmpty()) return sortedIndices.firstOrNull() ?: 0

        // Sample from nucleus
        val nucleusProbs = nucleus.map { probs[it] }
        val nucleusSum = nucleusProbs.sum()
        if (nucleusSum == 0f) return nucleus.first()
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
