package sk.ainet.samples.emoji.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CalibrationTest {
    @Test
    fun confidence_is_monotonic_and_crosses_the_threshold_at_the_reported_cosine() {
        val c = Calibration(k = 12.0, x0 = 0.4)
        val values = (0..20).map { c.confidence(it / 20.0) }
        assertTrue(values.zipWithNext().all { (a, b) -> b >= a }, "monotonic: $values")
        assertEquals(0.5, c.confidence(c.cosineAtThreshold), 1e-9)
        assertTrue(c.isTop(c.cosineAtThreshold + 1e-6))
        assertTrue(!c.isTop(c.cosineAtThreshold - 1e-6))
    }

    @Test
    fun fit_separates_two_clusters() {
        val positives = listOf(0.62, 0.70, 0.55, 0.66, 0.58, 0.74).map { it to true }
        val negatives = listOf(0.10, 0.22, 0.31, 0.18, 0.27, 0.35, 0.05).map { it to false }
        val fitted = Calibration.fit(positives + negatives, steps = 3000)
        assertTrue(fitted.k > 0, "slope positive: $fitted")
        assertTrue(fitted.cosineAtThreshold in 0.35..0.55, "boundary between the clusters: $fitted")
        assertEquals(1.0, fitted.accuracy, 1e-9)
        assertEquals(1.0, fitted.f1, 1e-9)
        assertFailsWith<IllegalArgumentException> { Calibration.fit(positives) }
    }

    @Test
    fun json_round_trip() {
        val c = Calibration(k = 9.5, x0 = 0.41, threshold = 0.6, maxTop = 8, queries = 30, accuracy = 0.93, f1 = 0.4)
        assertEquals(c, Calibration.parse(Calibration.encode(c)))
    }
}
