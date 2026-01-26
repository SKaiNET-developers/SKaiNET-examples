package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.currentTimeMillis

/**
 * Collector for token generation statistics with rolling window TPS calculation.
 */
class TokenStatisticsCollector(
    private val windowSize: Int = 10
) {
    private val timestamps = mutableListOf<Long>()
    private var tokenCount = 0
    private var startTimeMs: Long = 0
    private var promptTokenCount = 0

    /**
     * Start tracking a new generation session.
     */
    fun start(promptTokens: Int = 0) {
        timestamps.clear()
        tokenCount = 0
        promptTokenCount = promptTokens
        startTimeMs = currentTimeMillis()
    }

    /**
     * Record a newly generated token.
     */
    fun recordToken() {
        tokenCount++
        timestamps.add(currentTimeMillis())
        if (timestamps.size > windowSize) {
            timestamps.removeAt(0)
        }
    }

    /**
     * Get the current tokens-per-second rate using a rolling window.
     */
    fun getCurrentTps(): Float {
        if (timestamps.size < 2) return 0f
        val span = timestamps.last() - timestamps.first()
        if (span == 0L) return 0f
        return (timestamps.size - 1) * 1000f / span
    }

    /**
     * Get the average TPS over the entire generation.
     */
    fun getAverageTps(): Float {
        val elapsed = currentTimeMillis() - startTimeMs
        if (elapsed == 0L || tokenCount == 0) return 0f
        return tokenCount * 1000f / elapsed
    }

    /**
     * Get the total elapsed time in milliseconds.
     */
    fun getElapsedMs(): Long = currentTimeMillis() - startTimeMs

    /**
     * Get the total number of tokens generated.
     */
    fun getTokenCount(): Int = tokenCount

    /**
     * Build the current inference statistics snapshot.
     */
    fun buildStatistics(): InferenceStatistics {
        return InferenceStatistics(
            tokensGenerated = tokenCount,
            tokensPerSecond = getCurrentTps(),
            totalTimeMs = getElapsedMs(),
            promptTokens = promptTokenCount
        )
    }

    /**
     * Reset all statistics.
     */
    fun reset() {
        timestamps.clear()
        tokenCount = 0
        startTimeMs = 0
        promptTokenCount = 0
    }
}
