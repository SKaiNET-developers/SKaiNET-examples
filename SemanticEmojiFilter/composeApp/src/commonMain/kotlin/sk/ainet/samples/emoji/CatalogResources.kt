package sk.ainet.samples.emoji

import org.jetbrains.compose.resources.ExperimentalResourceApi
import semanticemojifilter.composeapp.generated.resources.Res
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.Catalog
import sk.ainet.samples.emoji.engine.CatalogIndex
import sk.ainet.samples.emoji.engine.EmbeddingEngine

/** The bundled catalog, calibration and (optionally) precomputed embeddings. */
@OptIn(ExperimentalResourceApi::class)
object CatalogResources {
    suspend fun catalog(): Catalog = Catalog.parse(Res.readBytes("files/catalog/catalog.json").decodeToString())

    suspend fun calibration(): Calibration =
        runCatching { Calibration.parse(Res.readBytes("files/catalog/calibration.json").decodeToString()) }.getOrDefault(Calibration.DEFAULT)

    /**
     * The index for [catalog]: the shipped `catalog-embeddings.bin` when its digest matches the
     * catalog (the web build always takes this path), otherwise computed with [engine].
     */
    suspend fun index(catalog: Catalog, engine: EmbeddingEngine, forceCompute: Boolean = false, onProgress: (String) -> Unit): CatalogIndex {
        if (!forceCompute && catalog.embeddingsSha256.isNotBlank()) {
            val bytes = runCatching { Res.readBytes("files/catalog/catalog-embeddings.bin") }.getOrNull()
            if (bytes != null && bytes.size == catalog.items.size * engine.dimensions * 4) {
                onProgress("Using precomputed catalog embeddings…")
                return CatalogIndex.fromBytes(catalog.items, bytes, engine.dimensions)
            }
        }
        return CatalogIndex.build(catalog.items, engine) { done, total -> if (done % 10 == 0 || done == total) onProgress("Embedding catalog… $done / $total") }
    }
}
