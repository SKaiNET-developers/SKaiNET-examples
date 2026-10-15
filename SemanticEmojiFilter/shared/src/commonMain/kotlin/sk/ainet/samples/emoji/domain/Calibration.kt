package sk.ainet.samples.emoji.domain

import kotlin.math.exp
import kotlin.math.ln
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Turns a cosine similarity into a match confidence with a logistic curve,
 * `confidence = 1 / (1 + exp(-k (cos - x0)))`. The two numbers are fitted on a small labelled
 * query set by the CLI (`calibrate`) and shipped next to the catalog: cosine thresholds are
 * encoder-specific, so the PRD's guessed `k = 10, x0 = 0.5` is replaced by measured values.
 */
@Serializable
data class Calibration(
    val k: Double,
    val x0: Double,
    val threshold: Double = 0.5,
    /** At most this many items form the top tier, however many pass [threshold]. */
    val maxTop: Int = 12,
    /** How many labelled queries the fit used; informational. */
    val queries: Int = 0,
    /** Training accuracy at [threshold]; informational. */
    val accuracy: Double = 0.0,
    /** Training F1 at [threshold]; informational. */
    val f1: Double = 0.0,
) {
    init {
        require(k > 0) { "k must be positive" }
        require(threshold in 0.0..1.0) { "threshold must be within [0, 1]" }
        require(maxTop > 0) { "maxTop must be positive" }
    }

    fun confidence(cosine: Double): Double = 1.0 / (1.0 + exp(-k * (cosine - x0)))

    fun isTop(cosine: Double): Boolean = confidence(cosine) >= threshold

    /** The cosine at which the confidence crosses [threshold]. */
    val cosineAtThreshold: Double get() = x0 - ln(1.0 / threshold - 1.0) / k

    companion object {
        /** Before calibration ran: a steep curve around a typical retrieval-encoder boundary. */
        val DEFAULT = Calibration(k = 14.0, x0 = 0.45)

        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Calibration = json.decodeFromString(serializer(), text)
        fun encode(c: Calibration): String = Json { prettyPrint = true }.encodeToString(serializer(), c)

        /**
         * One-dimensional logistic regression by gradient descent on `(cosine, isMatch)` pairs,
         * class-balanced: every positive weighs `negatives / positives`, otherwise the few matches
         * among hundreds of non-matches push the boundary above every real cosine (the unbalanced
         * fit on this catalog accepted nothing). Returns `k = slope`, `x0 = -intercept / slope`.
         */
        fun fit(samples: List<Pair<Double, Boolean>>, steps: Int = 4000, learningRate: Double = 0.5): Calibration {
            val positives = samples.count { it.second }
            val negatives = samples.size - positives
            require(positives > 0 && negatives > 0) { "calibration needs positives and negatives" }
            val wPos = negatives.toDouble() / positives
            val total = 2.0 * negatives
            var a = 10.0
            var b = -5.0
            repeat(steps) {
                var ga = 0.0
                var gb = 0.0
                for ((x, y) in samples) {
                    val p = 1.0 / (1.0 + exp(-(a * x + b)))
                    val w = if (y) wPos else 1.0
                    val err = w * (p - (if (y) 1.0 else 0.0))
                    ga += err * x
                    gb += err
                }
                a -= learningRate * ga / total
                b -= learningRate * gb / total
            }
            // The curve maps cosine to a probability; the threshold on it is the F1-optimal cut.
            val curve = Calibration(k = a, x0 = -b / a)
            var best = curve
            var bestF1 = -1.0
            for (step in 5..95 step 5) {
                val candidate = curve.copy(threshold = step / 100.0)
                val f1 = f1(candidate, samples)
                if (f1 > bestF1) {
                    bestF1 = f1
                    best = candidate
                }
            }
            val correct = samples.count { (x, y) -> best.isTop(x) == y }
            return best.copy(accuracy = correct / samples.size.toDouble(), f1 = bestF1)
        }

        private fun f1(c: Calibration, samples: List<Pair<Double, Boolean>>): Double {
            var tp = 0
            var fp = 0
            var fn = 0
            for ((x, y) in samples) {
                val predicted = c.isTop(x)
                if (predicted && y) tp++ else if (predicted) fp++ else if (y) fn++
            }
            return if (tp == 0) 0.0 else 2.0 * tp / (2.0 * tp + fp + fn)
        }
    }
}
