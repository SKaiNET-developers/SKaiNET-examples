package sk.ainet.samples.emoji

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.engine.CatalogIndex

@Serializable
data class GoldenQuery(val query: String, val vector: List<Float>, val expectedTop: List<String>)

/**
 * Ranks the recorded query vectors against the shipped catalog embeddings: exercises the sweep,
 * the calibration and the catalog files together without loading the model.
 */
class RankingGoldenTest {
    private val goldenFile = File(CatalogFiles.dir, "golden-queries.json")

    @Test
    fun recorded_queries_rank_their_expected_items_first() {
        if (!goldenFile.isFile || !CatalogFiles.embeddings.isFile) {
            println("golden files not generated yet (CLI embed-catalog + golden); skipping")
            return
        }
        val catalog = CatalogFiles.catalog
        val index = CatalogIndex.fromBytes(catalog.items, CatalogFiles.embeddings.readBytes())
        val calibration = Calibration.parse(File(CatalogFiles.dir, "calibration.json").readText())
        val goldens = Json { ignoreUnknownKeys = true }.decodeFromString<List<GoldenQuery>>(goldenFile.readText())
        assertTrue(goldens.size >= 3)
        for (g in goldens) {
            val ranked = index.rank(g.vector.toFloatArray(), calibration)
            assertEquals(g.expectedTop, ranked.take(g.expectedTop.size).map { it.item.id }, "top of '${g.query}'")
            assertTrue(ranked.first().confidence >= ranked.last().confidence, "confidence follows cosine for '${g.query}'")
        }
    }

    @Test
    fun the_showcase_pair_is_ordered_as_the_demo_claims() {
        if (!goldenFile.isFile || !CatalogFiles.embeddings.isFile) return
        val catalog = CatalogFiles.catalog
        val index = CatalogIndex.fromBytes(catalog.items, CatalogFiles.embeddings.readBytes())
        val calibration = Calibration.parse(File(CatalogFiles.dir, "calibration.json").readText())
        val healthy = Json { ignoreUnknownKeys = true }.decodeFromString<List<GoldenQuery>>(goldenFile.readText()).first { it.query == "healthy things to eat" }
        val ranked = index.rank(healthy.vector.toFloatArray(), calibration)
        val salad = ranked.indexOfFirst { it.item.id == "1F957" }
        val pizza = ranked.indexOfFirst { it.item.id == "1F355" }
        assertTrue(salad in 0 until 10, "salad is in the top ten for a healthy query (rank $salad)")
        assertTrue(salad < pizza, "salad ranks above pizza by vector alone (salad $salad, pizza $pizza)")
    }
}
