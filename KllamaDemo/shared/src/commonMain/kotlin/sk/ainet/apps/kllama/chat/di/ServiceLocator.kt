package sk.ainet.apps.kllama.chat.di

import sk.ainet.apps.kllama.chat.runtime.LlamaRuntime
import sk.ainet.apps.kllama.chat.data.repository.ModelRepositoryImpl
import sk.ainet.apps.kllama.chat.data.repository.PlatformModelLoader
import sk.ainet.apps.kllama.chat.data.repository.StubModelLoader
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository
import sk.ainet.apps.kllama.chat.inference.LlamaInferenceEngine

/**
 * Simple service locator for dependency injection.
 * Provides singleton instances of services.
 */
object ServiceLocator {

    private var platformLoader: PlatformModelLoader? = null
    private var modelRepository: ModelRepository? = null
    private var currentRuntime: LlamaRuntime? = null

    /**
     * Initialize the service locator with platform-specific implementations.
     */
    fun initialize(loader: PlatformModelLoader) {
        platformLoader = loader
        modelRepository = ModelRepositoryImpl(loader)
    }

    /**
     * Initialize with a stub loader for platforms without local inference support.
     */
    fun initializeWithStub(platformName: String) {
        val stubLoader = StubModelLoader(platformName)
        platformLoader = stubLoader
        modelRepository = ModelRepositoryImpl(stubLoader)
    }

    /**
     * Get the model repository instance.
     */
    fun getModelRepository(): ModelRepository {
        return modelRepository ?: throw IllegalStateException(
            "ServiceLocator not initialized. Call initialize() or initializeWithStub() first."
        )
    }

    /**
     * Get the platform loader (for accessing platform-specific features).
     */
    fun getPlatformLoader(): PlatformModelLoader? = platformLoader

    /**
     * Set the current LlamaRuntime (called by JvmModelLoader after loading).
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
     * Check if the service locator has been initialized.
     */
    val isInitialized: Boolean
        get() = platformLoader != null

    /**
     * Reset the service locator (for testing).
     */
    fun reset() {
        platformLoader = null
        modelRepository = null
        currentRuntime = null
    }
}
