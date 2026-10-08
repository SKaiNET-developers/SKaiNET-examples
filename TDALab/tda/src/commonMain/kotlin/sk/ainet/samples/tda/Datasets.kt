package sk.ainet.samples.tda

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Synthetic point clouds with known topology, so the app works offline and the tests have ground truth. */
public object Datasets {

    public data class Entry(val name: String, val expected: String, val make: () -> PointCloud)

    public val all: List<Entry> = listOf(
        Entry("Circle", "β0 = 1, β1 = 1") { circle(n = 60, radius = 1f, noise = 0.03f) },
        Entry("Two circles", "β0 = 2 -> 1, β1 = 2") { twoCircles(n = 50) },
        Entry("Figure eight", "β0 = 1, β1 = 2") { figureEight(n = 50) },
        Entry("Three blobs", "β0 = 3 -> 1, β1 = 0") { blobs(n = 30, k = 3) },
        Entry("Uniform noise", "no persistent feature") { uniform(n = 80) },
    )

    public fun circle(n: Int, radius: Float = 1f, noise: Float = 0f, cx: Float = 0f, cy: Float = 0f, seed: Int = 1): PointCloud {
        val rnd = Random(seed)
        val out = FloatArray(2 * n)
        for (i in 0 until n) {
            val a = 2.0 * PI * i / n
            val r = radius + noise * gaussian(rnd)
            out[2 * i] = cx + (r * cos(a)).toFloat()
            out[2 * i + 1] = cy + (r * sin(a)).toFloat()
        }
        return PointCloud(out)
    }

    /** Two disjoint circles: two loops, and two components that merge late. */
    public fun twoCircles(n: Int, seed: Int = 2): PointCloud =
        circle(n, radius = 1f, noise = 0.03f, cx = -1.6f, seed = seed) +
            circle(n, radius = 0.6f, noise = 0.03f, cx = 1.4f, seed = seed + 1)

    /** Two touching circles: one component with two loops. */
    public fun figureEight(n: Int, seed: Int = 3): PointCloud =
        circle(n, radius = 1f, noise = 0.02f, cx = -1f, seed = seed) +
            circle(n, radius = 1f, noise = 0.02f, cx = 1f, seed = seed + 1)

    public fun blobs(n: Int, k: Int, spread: Float = 0.25f, seed: Int = 4): PointCloud {
        val rnd = Random(seed)
        val out = FloatArray(2 * n * k)
        for (c in 0 until k) {
            val a = 2.0 * PI * c / k
            val cx = (2.0 * cos(a)).toFloat(); val cy = (2.0 * sin(a)).toFloat()
            for (i in 0 until n) {
                val p = 2 * (c * n + i)
                out[p] = cx + spread * gaussian(rnd)
                out[p + 1] = cy + spread * gaussian(rnd)
            }
        }
        return PointCloud(out)
    }

    public fun uniform(n: Int, seed: Int = 5): PointCloud {
        val rnd = Random(seed)
        return PointCloud(FloatArray(2 * n) { rnd.nextFloat() * 2f - 1f })
    }

    private operator fun PointCloud.plus(other: PointCloud): PointCloud = PointCloud(coords + other.coords)

    private fun gaussian(rnd: Random): Float {
        val u = 1.0 - rnd.nextDouble(); val v = rnd.nextDouble()
        return (sqrt(-2.0 * ln(u)) * cos(2.0 * PI * v)).toFloat()
    }
}
