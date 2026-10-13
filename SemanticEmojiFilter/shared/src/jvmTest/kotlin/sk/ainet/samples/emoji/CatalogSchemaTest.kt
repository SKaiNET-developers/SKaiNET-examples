package sk.ainet.samples.emoji

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import sk.ainet.samples.emoji.domain.Catalog

/** The bundled catalog files, read relative to the module directory. */
object CatalogFiles {
    val dir: File = listOf("../composeApp/src/commonMain/composeResources/files/catalog", "composeApp/src/commonMain/composeResources/files/catalog")
        .map { File(it) }.first { it.isDirectory }
    val catalog: Catalog by lazy { Catalog.parse(File(dir, "catalog.json").readText()) }
    val embeddings: File get() = File(dir, "catalog-embeddings.bin")
}

class CatalogSchemaTest {
    private val allowedTags = setOf("food", "healthy", "indulgent", "health", "sick", "music", "rock-band", "animal", "weather", "nature", "sport", "travel", "emotion", "object", "everyday")

    @Test
    fun catalog_is_complete_and_bilingual() {
        val c = CatalogFiles.catalog
        assertTrue(c.items.size >= 150, "catalog has ${c.items.size} items")
        assertEquals(Catalog.EMBEDDING_TEXT_VERSION, c.embeddingTextVersion)
        for (item in c.items) {
            assertTrue(item.emoji.isNotBlank() && item.nameEn.isNotBlank() && item.nameDe.isNotBlank(), "names for ${item.id}")
            assertTrue(item.keywordsEn.isNotEmpty(), "English keywords for ${item.id}")
            assertTrue(item.tags.isNotEmpty() && item.tags.all { it in allowedTags }, "tags of ${item.id}: ${item.tags}")
            assertTrue(item.group.isNotBlank(), "group of ${item.id}")
        }
        assertTrue(c.items.any { it.id == "1F355" } && c.items.any { it.id == "1F957" }, "the showcase pair pizza and salad is present")
    }

    @Test
    fun shipped_embeddings_match_the_catalog_digest() {
        val c = CatalogFiles.catalog
        val bin = CatalogFiles.embeddings
        if (!bin.isFile) {
            println("catalog-embeddings.bin not generated yet (run the CLI embed-catalog); skipping")
            return
        }
        val bytes = bin.readBytes()
        assertEquals(c.items.size * 768 * 4, bytes.size, "one 768-d float32 row per item")
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(c.embeddingsSha256, sha, "catalog.json records the digest of the shipped embeddings")
    }
}
