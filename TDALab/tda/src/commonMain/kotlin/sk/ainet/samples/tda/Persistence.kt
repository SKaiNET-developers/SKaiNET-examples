package sk.ainet.samples.tda

/** One bar of the barcode: a homology class of dimension [dim] alive on `[birth, death)`. */
public data class PersistencePair(val dim: Int, val birth: Float, val death: Float) {
    public val isInfinite: Boolean get() = death == Float.POSITIVE_INFINITY
    public val persistence: Float get() = death - birth
}

/** The persistence diagram of a Vietoris-Rips filtration truncated at [threshold]. */
public class PersistenceDiagram(public val pairs: List<PersistencePair>, public val threshold: Float) {

    /** Betti number β[dim] at scale [eps]: classes born at or before [eps] that are still alive. */
    public fun betti(dim: Int, eps: Float): Int =
        pairs.count { it.dim == dim && it.birth <= eps && eps < it.death }

    /** Bars of one dimension, longest first; zero-length bars are dropped unless [includeZero]. */
    public fun bars(dim: Int, includeZero: Boolean = false): List<PersistencePair> =
        pairs.filter { it.dim == dim && (includeZero || it.persistence > 0f) }
            .sortedByDescending { it.persistence }
}

/**
 * Persistent homology of the Vietoris-Rips complex over ℤ/2, dimensions 0 and 1.
 *
 * - **H0** by union-find over the sorted edges: an edge that joins two components kills the
 *   younger one at its length; the last component lives forever.
 * - **H1** by boundary-matrix reduction of the 2-simplices. Rows are edges (by filtration
 *   index), each triangle's column is its three edges, and columns are reduced left-to-right by
 *   XOR-ing in the stored column that already owns the same lowest row. A reduced column with
 *   pivot `e` pairs the class born at edge `e` with the triangle that fills it. Edges that close
 *   a cycle and never become a pivot are infinite bars.
 *
 * Triangles are generated per maximal edge (`e = (i, j)` with both `(i, k)`, `(j, k)` earlier in
 * the filtration), so they arrive in filtration order without ever being materialised. When
 * the triangle's own maximal edge is still unclaimed, the pair is apparent and costs O(1); that
 * is the bulk of them. Everything here is plain Kotlin on `IntArray`s: ℤ/2 column reduction is
 * sparse integer work, not tensor work.
 */
public object VietorisRips {

    public fun persistence(
        d: DistanceMatrix,
        filtration: Filtration = Filtration.build(d),
        threshold: Float = d.enclosingRadius,
    ): PersistenceDiagram {
        val n = d.size
        val pairs = ArrayList<PersistencePair>()
        if (n == 0) return PersistenceDiagram(pairs, threshold)
        val m = filtration.countEdgesUpTo(threshold)
        val len = filtration.edgeLength

        // ---- H0: union-find, and mark the edges that close a cycle (positive edges).
        val parent = IntArray(n) { it }
        fun find(x: Int): Int {
            var cur = x
            while (parent[cur] != cur) { parent[cur] = parent[parent[cur]]; cur = parent[cur] }
            return cur
        }
        val positive = BooleanArray(m)
        for (e in 0 until m) {
            val a = find(filtration.edgeI[e]); val b = find(filtration.edgeJ[e])
            if (a != b) { parent[a] = b; pairs.add(PersistencePair(0, 0f, len[e])) }
            else positive[e] = true
        }
        // Components never joined within the threshold live forever.
        var components = 0
        for (v in 0 until n) if (find(v) == v) components++
        repeat(components) { pairs.add(PersistencePair(0, 0f, Float.POSITIVE_INFINITY)) }

        // ---- H1: reduce triangle columns over the edge rows.
        val pivotOwner = IntArray(m) { -1 }          // pivot edge -> index into columns
        val columns = ArrayList<IntArray>()
        for (e in 0 until m) {
            val i = filtration.edgeI[e]; val j = filtration.edgeJ[e]
            for (k in 0 until n) {
                if (k == i || k == j) continue
                val a = filtration.edgeIndex(i, k); val b = filtration.edgeIndex(j, k)
                if (a >= e || b >= e) continue       // not a cofacet with e as its maximal edge
                // Apparent pair: the triangle's own top edge is still free, so it is the pivot.
                if (pivotOwner[e] == -1) {
                    pivotOwner[e] = columns.size
                    columns.add(if (a < b) intArrayOf(a, b, e) else intArrayOf(b, a, e))
                    pairs.add(PersistencePair(1, len[e], len[e]))
                    continue
                }
                var col = if (a < b) intArrayOf(a, b, e) else intArrayOf(b, a, e)
                while (col.isNotEmpty()) {
                    val low = col[col.size - 1]
                    val owner = pivotOwner[low]
                    if (owner == -1) {
                        pivotOwner[low] = columns.size
                        columns.add(col)
                        pairs.add(PersistencePair(1, len[low], len[e]))
                        break
                    }
                    col = xor(col, columns[owner])
                }
                // An empty column is a 2-cycle (H2 birth); not tracked here.
            }
        }
        for (e in 0 until m) if (positive[e] && pivotOwner[e] == -1) {
            pairs.add(PersistencePair(1, len[e], Float.POSITIVE_INFINITY))
        }
        return PersistenceDiagram(pairs, threshold)
    }

    /** Symmetric difference of two sorted index lists: ℤ/2 column addition. */
    internal fun xor(a: IntArray, b: IntArray): IntArray {
        val out = IntArray(a.size + b.size)
        var ia = 0; var ib = 0; var k = 0
        while (ia < a.size && ib < b.size) {
            val x = a[ia]; val y = b[ib]
            when {
                x < y -> { out[k++] = x; ia++ }
                x > y -> { out[k++] = y; ib++ }
                else -> { ia++; ib++ }
            }
        }
        while (ia < a.size) out[k++] = a[ia++]
        while (ib < b.size) out[k++] = b[ib++]
        return out.copyOf(k)
    }
}
