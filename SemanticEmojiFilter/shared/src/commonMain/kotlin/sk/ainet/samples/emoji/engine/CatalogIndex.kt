package sk.ainet.samples.emoji.engine

import kotlin.math.sqrt
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.domain.RankedItem
import sk.ainet.samples.emoji.domain.Tier

/**
 * The catalog embeddings, one L2-normalised row per item, and the cosine sweep over them.
 * 180 × 768 floats is 553 KB: computed once at startup on desktop and Android, shipped as
 * `catalog-embeddings.bin` for the web build (and as the parity golden for the tests).
 */
class CatalogIndex(
    val items: List<CatalogItem>,
    /** Row-major `items.size × dimensions`. */
    val vectors: FloatArray,
    val dimensions: Int,
) {
    init {
        require(vectors.size == items.size * dimensions) { "expected ${items.size} × $dimensions floats, got ${vectors.size}" }
    }

    /** Cosine of every item against [query] (unit vector), in catalog order. */
    fun cosines(query: FloatArray): FloatArray {
        require(query.size == dimensions) { "query has ${query.size} dimensions, index has $dimensions" }
        val out = FloatArray(items.size)
        for (i in items.indices) {
            val base = i * dimensions
            var dot = 0f
            for (d in 0 until dimensions) dot += vectors[base + d] * query[d]
            out[i] = dot
        }
        return out
    }

    /** Every item scored and sorted by cosine; the top tier is what passes the threshold, at most `maxTop` items. */
    fun rank(query: FloatArray, calibration: Calibration): List<RankedItem> {
        val cos = cosines(query)
        val sorted = items.indices.sortedByDescending { cos[it] }
        return sorted.mapIndexed { rank, i ->
            val c = cos[i].toDouble()
            val top = rank < calibration.maxTop && calibration.isTop(c)
            RankedItem(items[i], cos[i], calibration.confidence(c).toFloat(), if (top) Tier.TOP else Tier.REST)
        }
    }

    /** The unscored catalog, for an empty query. */
    fun unranked(): List<RankedItem> = items.map { RankedItem(it, 0f, 0f, Tier.REST) }

    fun vector(index: Int): FloatArray = vectors.copyOfRange(index * dimensions, (index + 1) * dimensions)

    /** Little-endian float32, row-major, no header. */
    fun toBytes(): ByteArray {
        val out = ByteArray(vectors.size * 4)
        for (i in vectors.indices) {
            val bits = vectors[i].toRawBits()
            val o = i * 4
            out[o] = bits.toByte()
            out[o + 1] = (bits ushr 8).toByte()
            out[o + 2] = (bits ushr 16).toByte()
            out[o + 3] = (bits ushr 24).toByte()
        }
        return out
    }

    companion object {
        fun fromBytes(items: List<CatalogItem>, bytes: ByteArray, dimensions: Int = 768): CatalogIndex {
            require(bytes.size == items.size * dimensions * 4) { "embeddings file has ${bytes.size} bytes, expected ${items.size * dimensions * 4}" }
            val floats = FloatArray(bytes.size / 4)
            for (i in floats.indices) {
                val o = i * 4
                val bits = (bytes[o].toInt() and 0xFF) or
                    ((bytes[o + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[o + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[o + 3].toInt() and 0xFF) shl 24)
                floats[i] = Float.fromBits(bits)
            }
            return CatalogIndex(items, floats, dimensions)
        }

        /** Embeds every item with [engine]; [onProgress] gets `(done, total)`. */
        fun build(items: List<CatalogItem>, engine: EmbeddingEngine, onProgress: (Int, Int) -> Unit = { _, _ -> }): CatalogIndex {
            val dim = engine.dimensions
            val vectors = FloatArray(items.size * dim)
            items.forEachIndexed { i, item ->
                val v = engine.embed(item.embeddingText())
                require(v.size == dim) { "embedding of '${item.id}' has ${v.size} dimensions, expected $dim" }
                v.copyInto(vectors, i * dim)
                onProgress(i + 1, items.size)
            }
            return CatalogIndex(items, vectors, dim)
        }

        fun cosine(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            var na = 0f
            var nb = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                na += a[i] * a[i]
                nb += b[i] * b[i]
            }
            return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
        }
    }
}
