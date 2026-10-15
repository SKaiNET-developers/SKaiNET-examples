package sk.ainet.samples.tda

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FiltrationTest {
    private val square = PointCloud.of(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)

    @Test
    fun edgesAreSortedAndIndexed() {
        val f = Filtration.build(DistanceMatrix.plain(square))
        assertEquals(6, f.edgeCount)
        for (e in 1 until f.edgeCount) assertTrue(f.edgeLength[e - 1] <= f.edgeLength[e])
        // The four sides come first, the two diagonals last.
        assertEquals(1f, f.edgeLength[3]); assertNear(sqrt(2f), f.edgeLength[4])
        assertEquals(f.edgeIndex(0, 2), f.edgeIndex(2, 0))
        assertTrue(f.edgeIndex(0, 2) >= 4)
        assertEquals(-1, f.edgeIndex(1, 1))
    }

    @Test
    fun prefixQueries() {
        val f = Filtration.build(DistanceMatrix.plain(square))
        assertEquals(0, f.countEdgesUpTo(0.5f))
        assertEquals(4, f.countEdgesUpTo(1f))
        assertEquals(6, f.countEdgesUpTo(2f))
        val sides = f.edgesUpTo(1f)
        assertEquals(8, sides.size)
        assertContentEquals(intArrayOf(0, 1), sides.copyOfRange(0, 2))
    }
}
