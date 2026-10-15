package sk.ainet.samples.tda

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.context.data
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.matmul
import sk.ainet.lang.tensor.minus
import sk.ainet.lang.tensor.plus
import sk.ainet.lang.tensor.reshape
import sk.ainet.lang.tensor.sqrt
import sk.ainet.lang.tensor.sum
import sk.ainet.lang.tensor.t
import sk.ainet.lang.tensor.times
import sk.ainet.lang.types.FP32
import kotlin.math.sqrt

/**
 * Dense, symmetric `n × n` Euclidean distance matrix with a zero diagonal, row-major.
 *
 * [compute] builds it with SKaiNET tensor ops, [plain] with a Kotlin double loop; the tests
 * check that both agree. Everything downstream (filtration, persistence) reads [values] as a
 * plain `FloatArray`, which is what SKaiNET hands back from [sk.ainet.lang.tensor.data.TensorData.copyToFloatArray].
 */
public class DistanceMatrix(public val size: Int, public val values: FloatArray) {
    init {
        require(values.size == size * size) { "values.size=${values.size} must equal size²=${size * size}" }
    }

    public operator fun get(i: Int, j: Int): Float = values[i * size + j]

    /** Largest pairwise distance. */
    public val max: Float by lazy { var m = 0f; for (v in values) if (v > m) m = v; m }

    /**
     * Enclosing radius `min_i max_j d(i, j)`: the radius at which one ball covers every point,
     * so the Vietoris-Rips complex is a cone and all H0 and H1 classes are dead. It is the
     * natural upper end of the ε slider and the default filtration threshold (the same trick
     * Ripser uses to stop early).
     */
    public val enclosingRadius: Float by lazy {
        if (size == 0) return@lazy 0f
        var best = Float.POSITIVE_INFINITY
        for (i in 0 until size) {
            var rowMax = 0f
            for (j in 0 until size) { val v = this[i, j]; if (v > rowMax) rowMax = v }
            if (rowMax < best) best = rowMax
        }
        best
    }

    public companion object {
        /** Reference implementation: `hypot` over every pair, O(n²) scalar Kotlin. */
        public fun plain(points: PointCloud): DistanceMatrix {
            val n = points.size
            val out = FloatArray(n * n)
            for (i in 0 until n) {
                val xi = points.x(i); val yi = points.y(i)
                for (j in i + 1 until n) {
                    val dx = xi - points.x(j); val dy = yi - points.y(j)
                    val d = sqrt(dx * dx + dy * dy)
                    out[i * n + j] = d; out[j * n + i] = d
                }
            }
            return DistanceMatrix(n, out)
        }

        /**
         * The SKaiNET way: with `X` the `[n, 2]` coordinate tensor and `s = rowSum(X ⊙ X)`,
         *
         * ```
         * D² = s[:, None] + s[None, :] − 2 · X · Xᵀ
         * ```
         *
         * one `matmul`, two broadcasts, a `clamp` (FP32 cancellation can go slightly negative)
         * and a `sqrt`. The `n × n` work runs in the backend's vectorised kernels instead of a
         * Kotlin loop. The result is symmetrised and the diagonal zeroed before use.
         */
        public fun compute(points: PointCloud, ctx: ExecutionContext = DirectCpuExecutionContext()): DistanceMatrix {
            val n = points.size
            if (n == 0) return DistanceMatrix(0, FloatArray(0))
            val centered = points.centered()
            val x: Tensor<FP32, Float> = data<FP32, Float>(ctx) {
                tensor { shape(n, 2) { fromArray(centered.coords) } }
            }
            val squares = (x * x).sum(1)                       // [n]      ‖xᵢ‖²
            val col = squares.reshape(Shape(n, 1))            // [n, 1]
            val row = squares.reshape(Shape(1, n))            // [1, n]
            val gram = x.matmul(x.t())                        // [n, n]   xᵢ · xⱼ
            val d2 = (col + row) - (2f * gram)                // broadcast to [n, n]
            val d = ctx.ops.clamp(d2, 0f, Float.MAX_VALUE).sqrt()
            val raw = d.data.copyToFloatArray()
            // Enforce the invariants exactly, independent of rounding in the kernel.
            for (i in 0 until n) {
                raw[i * n + i] = 0f
                for (j in i + 1 until n) {
                    val v = raw[i * n + j]
                    raw[j * n + i] = v
                }
            }
            return DistanceMatrix(n, raw)
        }
    }
}
