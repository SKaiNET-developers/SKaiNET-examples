package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import sk.ainet.apps.kllama.chat.runtime.LlamaRuntime
import sk.ainet.apps.kllama.chat.data.model.ModelFormatDetector
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.io.gguf.llama.LlamaRuntimeWeights
import sk.ainet.io.gguf.llama.LlamaWeightLoader
import sk.ainet.io.gguf.llama.LlamaWeightMapper
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.lang.types.FP32
import java.io.File
import java.io.FileInputStream

/**
 * JVM-specific model loader using SKaiNET's LlamaRuntime.
 *
 * This loader uses SKaiNET's built-in GGUF loading and Llama inference runtime
 * for full local inference support.
 */
class JvmModelLoader : PlatformModelLoader {

    private var currentRuntime: LlamaRuntime? = null
    private var currentWeights: LlamaRuntimeWeights? = null
    private var currentPath: String? = null
    private val ctx = DirectCpuExecutionContext()

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
            ModelFormat.GGUF -> loadGgufFromSource({ source }, name, sizeBytes)
            ModelFormat.SAFETENSORS -> ModelLoadResult.Error("SafeTensors loading not yet implemented")
            ModelFormat.UNKNOWN -> ModelLoadResult.Error("Unknown model format")
        }
    }

    override suspend fun extractMetadata(path: String, format: ModelFormat): ModelMetadata? {
        return when (format) {
            ModelFormat.GGUF -> extractGgufMetadata(path)
            ModelFormat.SAFETENSORS -> null
            ModelFormat.UNKNOWN -> null
        }
    }

    override fun unload() {
        currentRuntime?.reset()
        currentRuntime = null
        currentWeights = null
        currentPath = null
        ServiceLocator.setRuntime(null)
    }

    private suspend fun loadGgufFromPath(path: String): ModelLoadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext ModelLoadResult.Error("File not found: $path")
            }

            val fileSizeMb = file.length() / 1024 / 1024
            val estimatedMemoryMb = fileSizeMb * 4  // FP32 expansion factor
            val availableMemoryMb = Runtime.getRuntime().maxMemory() / 1024 / 1024

            println("Loading GGUF model from: $path")
            println("File size: $fileSizeMb MB")
            println("Estimated memory (FP32 dequantized): ~${estimatedMemoryMb} MB")
            println("Available heap: $availableMemoryMb MB")

            if (estimatedMemoryMb > availableMemoryMb * 0.8) {
                println("WARNING: Model may exceed available memory! Consider using a smaller model or increasing heap size.")
            }

            // Use SKaiNET's LlamaWeightLoader with dequantization
            val loader = LlamaWeightLoader(
                sourceProvider = { FileInputStream(file).asSource().buffered() },
                quantPolicy = LlamaWeightLoader.QuantPolicy.DEQUANTIZE_TO_FP32
            )

            println("Loading weights (this may take a while for large models)...")
            val weights = loader.loadToMap<FP32, Float>(ctx)
            println("Mapping weights to runtime structure...")
            val runtimeWeights = LlamaWeightMapper.map(weights)

            println("Creating LlamaRuntime...")
            val runtime = LlamaRuntime(ctx, runtimeWeights)

            currentRuntime = runtime
            currentWeights = runtimeWeights
            currentPath = path

            // Register runtime with ServiceLocator for inference engine
            ServiceLocator.setRuntime(runtime)

            val llamaMeta = runtimeWeights.metadata
            val metadata = ModelMetadata(
                name = file.nameWithoutExtension,
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = file.length(),
                quantization = ModelFormatDetector.detectQuantization(file.name),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )

            println("Model loaded successfully!")
            println("  Architecture: ${llamaMeta.architecture}")
            println("  Layers: ${llamaMeta.blockCount}")
            println("  Context: ${llamaMeta.contextLength}")
            println("  Vocab: ${llamaMeta.vocabSize}")

            ModelLoadResult.Success(
                LoadedModel(
                    metadata = metadata,
                    modelPath = path,
                    format = ModelFormat.GGUF
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ModelLoadResult.Error("Failed to load GGUF file: ${e.message}", e)
        }
    }

    private suspend fun loadGgufFromSource(
        sourceProvider: () -> Source,
        name: String,
        sizeBytes: Long
    ): ModelLoadResult = withContext(Dispatchers.IO) {
        try {
            val loader = LlamaWeightLoader(
                sourceProvider = sourceProvider,
                quantPolicy = LlamaWeightLoader.QuantPolicy.DEQUANTIZE_TO_FP32
            )

            val weights = loader.loadToMap<FP32, Float>(ctx)
            val runtimeWeights = LlamaWeightMapper.map(weights)
            val runtime = LlamaRuntime(ctx, runtimeWeights)

            currentRuntime = runtime
            currentWeights = runtimeWeights
            currentPath = name

            // Register runtime with ServiceLocator for inference engine
            ServiceLocator.setRuntime(runtime)

            val llamaMeta = runtimeWeights.metadata
            val metadata = ModelMetadata(
                name = name.substringBeforeLast("."),
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = sizeBytes,
                quantization = ModelFormatDetector.detectQuantization(name),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )

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

    private suspend fun extractGgufMetadata(path: String): ModelMetadata? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            // Quick metadata extraction using LlamaWeightLoader
            val loader = LlamaWeightLoader(
                sourceProvider = { FileInputStream(file).asSource().buffered() },
                loadTensorData = false // Only load metadata
            )

            val weights = loader.loadToMap<FP32, Float>(ctx)
            val llamaMeta = weights.metadata

            ModelMetadata(
                name = file.nameWithoutExtension,
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = file.length(),
                quantization = ModelFormatDetector.detectQuantization(file.name),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun estimateParamCount(meta: sk.ainet.io.gguf.llama.LlamaModelMetadata): Long {
        // Estimate based on architecture: embedding + layers + output
        val embedding = meta.embeddingLength.toLong() * meta.vocabSize
        val perLayer = (
            // attention: q, k, v, o projections
            4L * meta.embeddingLength * meta.embeddingLength +
            // ffn: gate, up, down
            3L * meta.embeddingLength * meta.feedForwardLength +
            // norms
            2L * meta.embeddingLength
        )
        val output = meta.embeddingLength.toLong() * meta.vocabSize
        return embedding + (perLayer * meta.blockCount) + output
    }

    /**
     * Get the LlamaRuntime for inference.
     */
    fun getRuntime(): LlamaRuntime? = currentRuntime

    /**
     * Get the execution context.
     */
    fun getContext(): DirectCpuExecutionContext = ctx
}
