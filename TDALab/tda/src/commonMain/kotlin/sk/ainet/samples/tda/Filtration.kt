package sk.ainet.samples.tda

/**
 * The 1-skeleton of the Vietoris-Rips filtration: every edge `(i, j)`, `i < j`, sorted by
 * length with `(i, j)` as a deterministic tie-break. Position in this order is the edge's
 * *filtration index*; [edgeIndex] maps a pair back to it, which is what the persistence
 * reduction uses to name boundary-matrix rows.
 */
public class Filtration private constructor(
    public val pointCount: Int,
    public val edgeI: IntArray,
    public val edgeJ: IntArray,
    public val edgeLength: FloatArray,
    private val index: IntArray,
) {
    public val edgeCount: Int get() = edgeLength.size

    /** Filtration index of edge `(i, j)` (either order), or -1 on the diagonal. */
    public fun edgeIndex(i: Int, j: Int): Int = index[i * pointCount + j]

    /** Number of edges with length ≤ [eps]. */
    public fun countEdgesUpTo(eps: Float): Int {
        var lo = 0; var hi = edgeLength.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (edgeLength[mid] <= eps) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** Edges with length ≤ [eps] as a flat `[i0, j0, i1, j1, …]` array, for drawing. */
    public fun edgesUpTo(eps: Float): IntArray {
        val m = countEdgesUpTo(eps)
        val out = IntArray(2 * m)
        for (e in 0 until m) { out[2 * e] = edgeI[e]; out[2 * e + 1] = edgeJ[e] }
        return out
    }

    public companion object {
        public fun build(d: DistanceMatrix): Filtration {
            val n = d.size
            val m = n * (n - 1) / 2
            val ti = IntArray(m); val tj = IntArray(m); val tl = FloatArray(m)
            var k = 0
            for (i in 0 until n) for (j in i + 1 until n) { ti[k] = i; tj[k] = j; tl[k] = d[i, j]; k++ }
            val order = (0 until m).sortedWith(compareBy({ tl[it] }, { ti[it] }, { tj[it] }))
            val ei = IntArray(m); val ej = IntArray(m); val el = FloatArray(m)
            val index = IntArray(n * n) { -1 }
            order.forEachIndexed { e, t ->
                ei[e] = ti[t]; ej[e] = tj[t]; el[e] = tl[t]
                index[ti[t] * n + tj[t]] = e; index[tj[t] * n + ti[t]] = e
            }
            return Filtration(n, ei, ej, el, index)
        }
    }
}
