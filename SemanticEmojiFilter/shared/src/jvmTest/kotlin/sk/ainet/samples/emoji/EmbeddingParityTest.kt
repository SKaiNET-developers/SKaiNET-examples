package sk.ainet.samples.emoji

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sk.ainet.samples.emoji.engine.CatalogIndex
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.engine.LeafSource

@Serializable
data class ReferenceEmbedding(val text: String, val isQuery: Boolean = false, val vector: List<Float>)

/**
 * Model-gated (needs `LEAF_MODEL_DIR` with the checkpoint): re-embeds the catalog and checks
 * it against the shipped embeddings, and against reference vectors from `sentence-transformers`
 * when `reference-subset.json` is present.
 */
class EmbeddingParityTest {
    private val modelDir = System.getenv("LEAF_MODEL_DIR")?.takeIf { File(it, "model.safetensors").isFile }

    @Test
    fun shipped_embeddings_are_reproduced_by_the_engine() {
        val dir = modelDir ?: run { println("LEAF_MODEL_DIR not set; skipping"); return }
        if (!CatalogFiles.embeddings.isFile) { println("no catalog-embeddings.bin; skipping"); return }
        val engine = runBlocking { LeafEmbeddingEngine.load(LeafSource.Directory(dir)) }
        val catalog = CatalogFiles.catalog
        val shipped = CatalogIndex.fromBytes(catalog.items, CatalogFiles.embeddings.readBytes(), engine.dimensions)
        val rebuilt = CatalogIndex.build(catalog.items, engine)
        var worst = 1f
        for (i in catalog.items.indices) {
            val c = CatalogIndex.cosine(shipped.vector(i), rebuilt.vector(i))
            if (c < worst) worst = c
            assertTrue(c >= 0.999f, "item ${catalog.items[i].id}: cosine to shipped embedding $c")
        }
        println("parity: worst cosine to the shipped embeddings $worst")
    }

    @Test
    fun reference_vectors_from_sentence_transformers_agree() {
        val dir = modelDir ?: return
        val ref = File(CatalogFiles.dir, "reference-subset.json").takeIf { it.isFile } ?: run { println("no reference-subset.json; skipping"); return }
        val engine = runBlocking { LeafEmbeddingEngine.load(LeafSource.Directory(dir)) }
        val refs = Json { ignoreUnknownKeys = true }.decodeFromString<List<ReferenceEmbedding>>(ref.readText())
        for (r in refs) {
            val ours = if (r.isQuery) engine.embedQuery(r.text) else engine.embed(r.text)
            assertEquals(r.vector.size, ours.size)
            val c = CatalogIndex.cosine(ours, r.vector.toFloatArray())
            assertTrue(c >= 0.99f, "'${r.text}': cosine to the reference $c")
        }
    }
}
