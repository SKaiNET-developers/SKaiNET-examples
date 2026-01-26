package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import sk.ainet.apps.kllama.chat.data.model.ModelFormatDetector
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.io.gguf.GGUFReader
import java.io.File
import java.io.FileInputStream

/**
 * JVM-specific model loader using SKaiNET's GGUF reader.
 */
class JvmModelLoader : PlatformModelLoader {

    private var currentReader: GGUFReader? = null
    private var currentPath: String? = null

    override suspend fun loadFromPath(path: String, format: ModelFormat): ModelLoadResult {
        return when (format) {
            ModelFormat.GGUF -> loadGgufFromPath(path)
            ModelFormat.SAFETENSORS -> ModelLoadResult.Error("SafeTensors loading not yet implemented")
            ModelFormat.UNKNOWN -> ModelLoadResult.Error("Unknown model format")
        }
    }

    override suspend fun loadFromSource(
        source: Source,
        name: String,
        sizeBytes: Long,
        format: ModelFormat
    ): ModelLoadResult {
        return when (format) {
            ModelFormat.GGUF -> loadGgufFromSource(source, name, sizeBytes)
            ModelFormat.SAFETENSORS -> ModelLoadResult.Error("SafeTensors loading not yet implemented")
            ModelFormat.UNKNOWN -> ModelLoadResult.Error("Unknown model format")
        }
    }

    override suspend fun extractMetadata(path: String, format: ModelFormat): ModelMetadata? {
        return when (format) {
            ModelFormat.GGUF -> extractGgufMetadata(path)
            ModelFormat.SAFETENSORS -> null // Not yet implemented
            ModelFormat.UNKNOWN -> null
        }
    }

    override fun unload() {
        currentReader = null
        currentPath = null
    }

    private fun loadGgufFromPath(path: String): ModelLoadResult {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return ModelLoadResult.Error("File not found: $path")
            }

            val source = FileInputStream(file).asSource().buffered()
            val reader = GGUFReader(source)

            currentReader = reader
            currentPath = path

            val metadata = extractMetadataFromReader(reader, file.name, file.length())

            ModelLoadResult.Success(
                LoadedModel(
                    metadata = metadata,
                    modelPath = path,
                    format = ModelFormat.GGUF
                )
            )
        } catch (e: Exception) {
            ModelLoadResult.Error("Failed to load GGUF file: ${e.message}", e)
        }
    }

    private fun loadGgufFromSource(source: Source, name: String, sizeBytes: Long): ModelLoadResult {
        return try {
            val reader = GGUFReader(source)

            currentReader = reader
            currentPath = name

            val metadata = extractMetadataFromReader(reader, name, sizeBytes)

            ModelLoadResult.Success(
                LoadedModel(
                    metadata = metadata,
                    modelPath = name,
                    format = ModelFormat.GGUF
                )
            )
        } catch (e: Exception) {
            ModelLoadResult.Error("Failed to load GGUF from source: ${e.message}", e)
        }
    }

    private fun extractGgufMetadata(path: String): ModelMetadata? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val source = FileInputStream(file).asSource().buffered()
            val reader = GGUFReader(source)

            extractMetadataFromReader(reader, file.name, file.length())
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMetadataFromReader(reader: GGUFReader, name: String, sizeBytes: Long): ModelMetadata {
        // Calculate parameter count from tensor data
        var paramCount = 0L
        val tensorNames = mutableListOf<String>()

        reader.tensors.forEach { tensor ->
            tensorNames.add(tensor.name)
            // Count elements in tensor data
            var count = 0L
            tensor.data.forEach { _ -> count++ }
            paramCount += count
        }

        // Try to infer model info from tensor names
        val modelName = inferModelName(tensorNames, name)
        val numLayers = inferNumLayers(tensorNames)

        return ModelMetadata(
            name = modelName,
            parameterCount = paramCount,
            sizeBytes = sizeBytes,
            quantization = ModelFormatDetector.detectQuantization(name),
            contextLength = 2048, // Default, can be overridden by config
            vocabSize = 32000,    // Default for Llama-style models
            hiddenSize = 0,       // Not easily extractable without metadata
            numLayers = numLayers,
            numHeads = 0          // Not easily extractable without metadata
        )
    }

    /**
     * Infer model name from tensor names or filename.
     */
    private fun inferModelName(tensorNames: List<String>, filename: String): String {
        // Try to detect architecture from tensor names
        val architecture = when {
            tensorNames.any { it.contains("model.layers") } -> "Llama"
            tensorNames.any { it.contains("transformer.h") } -> "GPT"
            tensorNames.any { it.contains("encoder.layer") } -> "BERT"
            else -> "GGUF Model"
        }

        // Use filename without extension
        val baseName = filename.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")

        return if (baseName.isNotBlank() && baseName.length > 3) {
            baseName
        } else {
            architecture
        }
    }

    /**
     * Infer number of layers from tensor names.
     */
    private fun inferNumLayers(tensorNames: List<String>): Int {
        // Look for layer indices in tensor names
        val layerPattern = Regex("\\.(\\d+)\\.")
        val layerIndices = tensorNames.mapNotNull { name ->
            layerPattern.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        return if (layerIndices.isNotEmpty()) {
            layerIndices.maxOrNull()?.plus(1) ?: 0
        } else {
            0
        }
    }

    /**
     * Get the current GGUF reader for inference.
     */
    fun getReader(): GGUFReader? = currentReader
}
