package sk.ainet.samples.emoji.domain

/** Where an item lands after a sweep: above the calibrated threshold or below. */
enum class Tier { TOP, REST }

/** One catalog item scored against the current query. */
data class RankedItem(
    val item: CatalogItem,
    /** Cosine similarity between the query and the item embedding (both L2-normalised). */
    val cosine: Float,
    /** Calibrated match confidence in `[0, 1]`, see [Calibration]. */
    val confidence: Float,
    val tier: Tier,
)
