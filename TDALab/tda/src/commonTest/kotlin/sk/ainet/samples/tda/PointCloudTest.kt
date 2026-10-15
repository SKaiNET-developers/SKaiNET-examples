package sk.ainet.samples.tda

import kotlin.test.Test
import kotlin.test.assertEquals

class PointCloudTest {
    @Test
    fun parsesCsvWithHeaderBlanksAndMixedSeparators() {
        val text = """
            x,y
            0,0

            1;0
            # comment
            0.5 0.5
            nan,1
            1	1
        """.trimIndent()
        val cloud = PointCloud.parseCsv(text)
        assertEquals(4, cloud.size)
        assertEquals(1f, cloud.x(1)); assertEquals(0f, cloud.y(1))
        assertEquals(1f, cloud.x(3)); assertEquals(1f, cloud.y(3))
    }

    @Test
    fun boundsAndCentering() {
        val cloud = PointCloud.of(0f, 0f, 2f, 4f)
        assertEquals(Bounds(0f, 2f, 0f, 4f), cloud.bounds)
        val c = cloud.centered()
        assertEquals(-1f, c.x(0)); assertEquals(-2f, c.y(0))
        assertEquals(1f, c.x(1)); assertEquals(2f, c.y(1))
    }

    @Test
    fun subsampleKeepsAtMostMaxPoints() {
        val cloud = Datasets.uniform(100)
        assertEquals(30, cloud.subsample(30).size)
        assertEquals(100, cloud.subsample(500).size)
    }

    @Test
    fun formatIsFixedPoint() {
        assertEquals("1.50", 1.5f.format())
        assertEquals("0.001", 0.0011f.format(3))
        assertEquals("-2.25", (-2.25f).format())
        assertEquals("∞", Float.POSITIVE_INFINITY.format())
    }
}
