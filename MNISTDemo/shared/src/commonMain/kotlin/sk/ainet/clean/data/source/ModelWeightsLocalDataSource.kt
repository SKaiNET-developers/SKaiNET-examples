package sk.ainet.clean.data.source

import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.domain.model.ModelId

/** Reads model weights from bundled assets/resources (PRD §4, §7). */
class ModelWeightsLocalDataSource(
    private val resourceReader: ResourceReader,
    private val pathResolver: (ModelId) -> String,
) : ModelWeightsDataSource {
    override suspend fun read(modelId: ModelId): ByteArray? {
        val path = pathResolver(modelId)
        return resourceReader.read(path)
    }
}
