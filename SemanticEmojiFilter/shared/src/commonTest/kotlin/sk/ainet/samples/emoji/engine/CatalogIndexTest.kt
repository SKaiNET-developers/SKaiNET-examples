package sk.ainet.samples.emoji.engine

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.domain.Tier

class CatalogIndexTest {
    private val items = listOf(
        CatalogItem("A", "🍕", "pizza", "Pizza", listOf("cheese", "slice"), listOf("Käse")),
        CatalogItem("B", "🥗", "green salad", "Salat", listOf("food", "green"), listOf("gesund")),
        CatalogItem("C", "🎸", "guitar", "Gitarre", listOf("music"), listOf("Musik")),
    )

    private fun unit(vararg v: Float): FloatArray {
        val n = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
        return FloatArray(v.size) { v[it] / n }
    }

    private val vectors = unit(1f, 0f, 0f) + unit(0.8f, 0.6f, 0f) + unit(0f, 0f, 1f)
    private val index = CatalogIndex(items, vectors, dimensions = 3)
    private val calibration = Calibration(k = 20.0, x0 = 0.7)

    @Test
    fun rank_sorts_by_cosine_and_applies_the_tier() {
        val ranked = index.rank(unit(1f, 0f, 0f), calibration)
        assertEquals(listOf("A", "B", "C"), ranked.map { it.item.id })
        assertEquals(1f, ranked[0].cosine, 1e-6f)
        assertEquals(0.8f, ranked[1].cosine, 1e-6f)
        assertEquals(listOf(Tier.TOP, Tier.TOP, Tier.REST), ranked.map { it.tier })
        val capped = index.rank(unit(1f, 0f, 0f), calibration.copy(maxTop = 1))
        assertEquals(listOf(Tier.TOP, Tier.REST, Tier.REST), capped.map { it.tier }, "maxTop caps the top tier")
    }

    @Test
    fun bytes_round_trip_little_endian() {
        val restored = CatalogIndex.fromBytes(items, index.toBytes(), dimensions = 3)
        assertContentEquals(index.vectors, restored.vectors)
        assertEquals(3, restored.dimensions)
    }

    @Test
    fun build_uses_the_shared_embedding_text() {
        val seen = ArrayList<String>()
        val engine = object : EmbeddingEngine {
            override val dimensions = 3
            override fun embed(text: String): FloatArray { seen += text; return unit(1f, 1f, 1f) }
        }
        val built = CatalogIndex.build(items, engine)
        assertEquals(listOf("pizza. cheese, slice. Pizza", "green salad. food, green. Salat", "guitar. music. Gitarre"), seen)
        assertEquals(9, built.vectors.size)
    }
}
