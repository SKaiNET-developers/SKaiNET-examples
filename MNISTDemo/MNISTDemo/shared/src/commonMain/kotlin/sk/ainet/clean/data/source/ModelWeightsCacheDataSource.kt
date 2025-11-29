package sk.ainet.clean.data.source

import sk.ainet.clean.domain.model.ModelId

/** Simple in-memory cache (optional per PRD §4). */
class ModelWeightsCacheDataSource : ModelWeightsDataSource {
    private val map = mutableMapOf<String, ByteArray>()

    override suspend fun read(modelId: ModelId): ByteArray? = map[modelId.value]

    suspend fun put(modelId: ModelId, bytes: ByteArray) {
        map[modelId.value] = bytes
    }
}
