package sk.ainet.samples.tda

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) =
    assertTrue(abs(expected - actual) < eps, "expected $expected, actual $actual")

class DistanceMatrixTest {

    private val square = PointCloud.of(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)

    @Test
    fun plainUnitSquare() {
        val d = DistanceMatrix.plain(square)
        assertEquals(1f, d[0, 1]); assertEquals(0f, d[3, 3])
        // sqrt(2f) is evaluated in double precision on JS, the array holds float32: compare with tolerance.
        assertNear(sqrt(2f), d[0, 2])
        assertNear(sqrt(2f), d.max)
        assertNear(sqrt(2f), d.enclosingRadius)
    }

    @Test
    fun skainetMatchesPlainOnUnitSquare() {
        val a = DistanceMatrix.plain(square)
        val b = DistanceMatrix.compute(square)
        for (i in 0 until 4) for (j in 0 until 4) {
            assertTrue(abs(a[i, j] - b[i, j]) < 1e-5f, "d[$i,$j] plain=${a[i, j]} skainet=${b[i, j]}")
        }
    }

    @Test
    fun skainetMatchesPlainOnRandomCloudFarFromOrigin() {
        // Offset coordinates so the centering step matters for FP32 cancellation.
        val base = Datasets.uniform(n = 60, seed = 11)
        val shifted = PointCloud(FloatArray(base.coords.size) { base.coords[it] + 50f })
        val a = DistanceMatrix.plain(shifted)
        val b = DistanceMatrix.compute(shifted)
        var worst = 0f
        for (i in 0 until a.size) for (j in 0 until a.size) {
            worst = maxOf(worst, abs(a[i, j] - b[i, j]))
            assertEquals(b[i, j], b[j, i], "symmetry at $i,$j")
        }
        assertTrue(worst < 2e-4f, "max abs difference $worst")
        for (i in 0 until a.size) assertEquals(0f, b[i, i])
    }

    @Test
    fun enclosingRadiusOfCircleIsAboutItsDiameter() {
        val d = DistanceMatrix.compute(Datasets.circle(n = 40))
        assertTrue(abs(d.enclosingRadius - 2f) < 0.02f, "enclosing radius ${d.enclosingRadius}")
    }
}
