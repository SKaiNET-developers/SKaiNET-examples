package sk.ainet.samples.tda

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistenceTest {

    private fun diagram(points: PointCloud) = VietorisRips.persistence(DistanceMatrix.compute(points))

    @Test
    fun xorIsSymmetricDifference() {
        assertContentEquals(intArrayOf(1, 4), VietorisRips.xor(intArrayOf(1, 2, 3), intArrayOf(2, 3, 4)))
        assertContentEquals(intArrayOf(), VietorisRips.xor(intArrayOf(5, 7), intArrayOf(5, 7)))
    }

    @Test
    fun unitSquare() {
        // Sides at 1, diagonals at √2. The 4th side closes the loop at ε = 1; the first
        // diagonal creates two triangles that fill it at ε = √2.
        val dg = diagram(PointCloud.of(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
        val h0 = dg.pairs.filter { it.dim == 0 }
        assertEquals(3, h0.count { it.death == 1f })
        assertEquals(1, h0.count { it.isInfinite })
        val h1 = dg.bars(1)
        assertEquals(1, h1.size)
        assertEquals(1f, h1[0].birth)
        assertTrue(abs(h1[0].death - sqrt(2f)) < 1e-5f)

        assertEquals(4, dg.betti(0, 0.5f))
        assertEquals(1, dg.betti(0, 1f))
        assertEquals(0, dg.betti(1, 0.99f))
        assertEquals(1, dg.betti(1, 1.2f))
        assertEquals(0, dg.betti(1, 1.5f))
    }

    @Test
    fun circleHasExactlyOneLongLoop() {
        val dg = diagram(Datasets.circle(n = 40, radius = 1f))
        val long = dg.bars(1).filter { it.persistence > 0.5f }
        assertEquals(1, long.size, "long H1 bars: ${dg.bars(1)}")
        // A regular 40-gon: the loop is born at the side length and dies near r·√3 (inscribed triangle).
        assertTrue(abs(long[0].death - sqrt(3f)) < 0.05f, "death ${long[0].death}")
        assertEquals(1, dg.betti(1, 1f))
        assertEquals(1, dg.betti(0, 1f))
        assertTrue(dg.bars(1).none { it.isInfinite })
    }

    @Test
    fun twoCirclesTwoLoopsTwoComponents() {
        val dg = diagram(Datasets.twoCircles(n = 40))
        assertEquals(2, dg.bars(1).count { it.persistence > 0.3f }, "H1 ${dg.bars(1).take(4)}")
        // Circles of radius 1 (centre −1.6) and 0.6 (centre 1.4): two components until the
        // 1.4 gap between them is bridged.
        assertEquals(2, dg.betti(0, 1.0f))
        assertEquals(1, dg.betti(0, 2.0f))
        assertEquals(2, dg.betti(1, 0.6f))
    }

    @Test
    fun blobsHaveNoPersistentLoop() {
        val dg = diagram(Datasets.blobs(n = 25, k = 3))
        assertTrue(dg.bars(1).all { it.persistence < 0.3f }, "H1 ${dg.bars(1).take(3)}")
        assertEquals(3, dg.betti(0, 1.0f))
        assertEquals(1, dg.betti(0, 3.5f))
    }

    @Test
    fun betti0CountsComponentsExactly() {
        val cloud = PointCloud.of(0f, 0f, 1f, 0f, 10f, 0f, 11f, 0f)
        val dg = diagram(cloud)
        assertEquals(4, dg.betti(0, 0f))
        assertEquals(2, dg.betti(0, 1f))
        assertEquals(1, dg.betti(0, 9f))
        assertEquals(0, dg.betti(1, 9f))
    }
}
