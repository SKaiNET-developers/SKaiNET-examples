package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult

/**
 * Stub model loader for platforms where SKaiNET inference is not available.
 * Returns errors indicating the platform limitation.
 */
class StubModelLoader(private val platformName: String) : PlatformModelLoader {

    override suspend fun loadFromPath(path: String, format: ModelFormat): ModelLoadResult {
        return ModelLoadResult.Error(
            "Model loading is not supported on $platformName. Please use the JVM/Desktop version for local inference."
        )
    }

    override suspend fun loadFromSource(
        source: Source,
        name: String,
        sizeBytes: Long,
        format: ModelFormat
    ): ModelLoadResult {
        return ModelLoadResult.Error(
            "Model loading is not supported on $platformName. Please use the JVM/Desktop version for local inference."
        )
    }

    override suspend fun extractMetadata(path: String, format: ModelFormat): ModelMetadata? {
        return null
    }

    override fun unload() {
        // No-op for stub
    }
}
