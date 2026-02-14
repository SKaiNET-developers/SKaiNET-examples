package sk.ainet.apps.kllama.chat.di

import sk.ainet.apps.kllama.chat.runtime.LlamaRuntime
import sk.ainet.apps.kllama.chat.data.repository.ModelRepositoryImpl
import sk.ainet.apps.kllama.chat.data.repository.PlatformModelLoader
import sk.ainet.apps.kllama.chat.data.source.ModelDataSource
import sk.ainet.apps.kllama.chat.data.source.ModelMetadataCacheDataSource
import sk.ainet.apps.kllama.chat.data.source.NoOpModelDataSource
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository
import sk.ainet.apps.kllama.chat.inference.LlamaInferenceEngine
import sk.ainet.apps.kllama.chat.logging.AppLogger

/**
 * Simple service locator for dependency injection.
 * Provides singleton instances of services.
 */
object ServiceLocator {

    private lateinit var platformLoader: PlatformModelLoader
    private lateinit var modelDataSource: ModelDataSource

    private val metadataCache by lazy { ModelMetadataCacheDataSource() }

    private val _modelRepository: ModelRepository by lazy {
        ModelRepositoryImpl(platformLoader, modelDataSource, metadataCache)
    }

    private var currentRuntime: LlamaRuntime? = null

    /**
     * Configure the service locator with platform-specific implementations.
     */
    fun configure(
        loader: PlatformModelLoader,
        dataSource: ModelDataSource = NoOpModelDataSource()
    ) {
        platformLoader = loader
        modelDataSource = dataSource
    }

    /**
     * Get the model repository instance.
     */
    fun getModelRepository(): ModelRepository {
        return _modelRepository
    }

    /**
     * Get the platform loader (for accessing platform-specific features).
     */
    fun getPlatformLoader(): PlatformModelLoader = platformLoader

    /**
     * Set the current LlamaRuntime (called by CommonModelLoader after loading).
     */
    fun setRuntime(runtime: LlamaRuntime?) {
        currentRuntime = runtime
    }

    /**
     * Get the current LlamaRuntime if available.
     */
    fun getRuntime(): LlamaRuntime? = currentRuntime

    /**
     * Create an inference engine for the given model.
     */
    fun createInferenceEngine(model: LoadedModel?): InferenceEngine {
        return LlamaInferenceEngine(model, runtime = currentRuntime)
    }

    /**
     * Get the inference engine factory.
     */
    fun getInferenceEngineFactory(): (LoadedModel?) -> InferenceEngine {
        return { model -> createInferenceEngine(model) }
    }

    /**
     * Get the AppLogger singleton.
     */
    fun getAppLogger(): AppLogger = AppLogger

    /**
     * Check if the service locator has been configured.
     */
    val isInitialized: Boolean
        get() = ::platformLoader.isInitialized

    /**
     * Reset the service locator (for testing).
     */
    fun reset() {
        currentRuntime = null
    }
}
