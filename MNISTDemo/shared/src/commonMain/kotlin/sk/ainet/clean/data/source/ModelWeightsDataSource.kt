package sk.ainet.clean.data.source

import sk.ainet.clean.domain.model.ModelId

/** Uniform contract for weight sources (PRD §4). */
interface ModelWeightsDataSource {
    /**
     * Read raw bytes for the given [modelId].
     * Return null when not present in this source.
     */
    suspend fun read(modelId: ModelId): ByteArray?
}
