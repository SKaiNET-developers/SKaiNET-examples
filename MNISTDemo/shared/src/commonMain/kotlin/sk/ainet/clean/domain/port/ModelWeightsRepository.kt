package sk.ainet.clean.domain.port

import sk.ainet.clean.domain.model.ModelId

/** Repository abstraction to retrieve model weights (PRD §2). */
interface ModelWeightsRepository {
    /** Obtain raw model weights for the given [modelId]. */
    suspend fun getWeights(modelId: ModelId): ByteArray

    /** Store raw model weights for the given [modelId]. */
    suspend fun putWeights(modelId: ModelId, weights: ByteArray)
}
