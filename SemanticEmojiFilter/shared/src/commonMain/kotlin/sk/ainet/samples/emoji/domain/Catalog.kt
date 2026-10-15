package sk.ainet.samples.emoji.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One emoji of the catalog: CLDR names and keywords in English and German, plus curated tags. */
@Serializable
data class CatalogItem(
    /** Hex code points joined with `-`, e.g. `1F355`; stable across catalog versions. */
    val id: String,
    val emoji: String,
    val nameEn: String,
    val nameDe: String,
    val keywordsEn: List<String> = emptyList(),
    val keywordsDe: List<String> = emptyList(),
    /** Hand-curated for the showcase groups (`healthy`, `indulgent`, `sick`, `rock-band`, …). */
    val tags: List<String> = emptyList(),
    val group: String = "",
) {
    /**
     * The single text the encoder sees for this item. CLI, tests and app share this definition:
     * the precomputed embeddings and the parity golden depend on it (see `Catalog.embeddingTextVersion`).
     */
    fun embeddingText(): String = buildString {
        append(nameEn)
        if (keywordsEn.isNotEmpty()) append(". ").append(keywordsEn.joinToString(", "))
        append(". ").append(nameDe)
    }

    fun name(german: Boolean): String = if (german) nameDe else nameEn
}

@Serializable
data class Catalog(
    val version: Int,
    /** CLDR release the names and keywords were generated from. */
    val cldr: String,
    /** Bump when [CatalogItem.embeddingText] changes; the shipped embeddings are only valid for the same version. */
    val embeddingTextVersion: Int,
    /** SHA-256 of `catalog-embeddings.bin` computed for these items, empty until the CLI wrote it. */
    val embeddingsSha256: String = "",
    val items: List<CatalogItem>,
) {
    init {
        require(items.isNotEmpty()) { "catalog has no items" }
        require(items.map { it.id }.toSet().size == items.size) { "catalog has duplicate ids" }
    }

    fun item(id: String): CatalogItem? = items.firstOrNull { it.id == id }

    companion object {
        const val EMBEDDING_TEXT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Catalog = json.decodeFromString(serializer(), text)
        fun encode(catalog: Catalog): String = Json { prettyPrint = true }.encodeToString(serializer(), catalog)
    }
}
