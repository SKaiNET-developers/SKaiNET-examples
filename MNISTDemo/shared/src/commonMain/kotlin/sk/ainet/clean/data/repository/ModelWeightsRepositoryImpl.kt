package sk.ainet.clean.data.repository

import sk.ainet.clean.data.RepositoryError
import sk.ainet.clean.data.RepositoryException
import sk.ainet.clean.data.source.ModelWeightsCacheDataSource
import sk.ainet.clean.data.source.ModelWeightsDataSource
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.ModelWeightsRepository

/** Repository composition following PRD §4 resolution order: cache → local → remote. */
class ModelWeightsRepositoryImpl(
    private val cache: ModelWeightsCacheDataSource?,
    private val local: ModelWeightsDataSource?,
    private val remote: ModelWeightsDataSource? = null,
) : ModelWeightsRepository {

    override suspend fun getWeights(modelId: ModelId): ByteArray {
        // 1) try cache
        cache?.read(modelId)?.let { return it }

        // 2) try local
        local?.read(modelId)?.let { bytes ->
            cache?.put(modelId, bytes)
            return bytes
        }

        // 3) try remote
        remote?.read(modelId)?.let { bytes ->
            cache?.put(modelId, bytes)
            return bytes
        }

        // If none provided or found
        throw RepositoryException(RepositoryError.NotFound(modelId.value))
    }
}
