package sk.ainet.clean.di

import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.data.repository.ModelWeightsRepositoryImpl
import sk.ainet.clean.data.source.ModelWeightsCacheDataSource
import sk.ainet.clean.data.source.ModelWeightsLocalDataSource
import sk.ainet.clean.domain.factory.DigitClassifierFactory
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import sk.ainet.clean.domain.port.InferenceModule

/**
 * Lightweight DI container (Service Locator) for the MNIST demo (PRD §6).
 *
 * Platform code should call [configure] early (e.g., in Application init or main())
 * to provide the platform-specific [ResourceReader] and inference module providers.
 */
object ServiceLocator {

    /** Lazily-initialized platform resource access. Must be provided via [configure]. */
    private lateinit var resourceReader: ResourceReader

    /** Providers for inference modules per strategy (CNN / MLP). Optional if [digitClassifierFactory] is provided directly. */
    private var cnnModuleProvider: (() -> InferenceModule)? = null
    private var mlpModuleProvider: (() -> InferenceModule)? = null

    /** Optionally injected factory (preferred). */
    private var injectedFactory: DigitClassifierFactory? = null

    /** Simple in-memory cache for weights, optional as per PRD §4. */
    private val cacheDataSource by lazy { ModelWeightsCacheDataSource() }

    /** Resolve resource file path from ModelId → bundled file names. */
    private val pathResolver: (ModelId) -> String = { modelId ->
        when (modelId.value) {
            ModelId.CNN_MNIST.value -> "files/mnist_cnn.gguf"
            ModelId.MLP_MNIST.value -> "files/mnist_mlp.gguf"
            else -> error("Unsupported ModelId: ${modelId.value}")
        }
    }

    /** Local (bundled) weights data source using the platform [ResourceReader]. */
    private val localDataSource by lazy {
        ensureConfigured()
        ModelWeightsLocalDataSource(resourceReader, pathResolver)
    }

    /** Repository composed as cache → local (→ remote not used for now). */
    val modelWeightsRepository by lazy {
        ModelWeightsRepositoryImpl(
            cache = cacheDataSource,
            local = localDataSource,
            remote = null,
        )
    }

    /** Factory to obtain DigitClassifier strategies. */
    val digitClassifierFactory: DigitClassifierFactory by lazy {
        ensureConfigured()
        injectedFactory ?: run {
            // If not injected, we cannot build a default without knowing how to create modules.
            // Provide a helpful error instructing platform to inject the factory.
            error("DigitClassifierFactory not injected. Call ServiceLocator.configure(...) with a DigitClassifierFactory.")
        }
    }

    /** Convenience to obtain a classifier for a given modelId (PRD §6). */
    fun provideDigitClassifier(modelId: ModelId): DigitClassifier =
        digitClassifierFactory.create(modelId)

    /**
     * Entry point for platform-specific wiring.
     * - [resourceReader]: how to read bundled resources on the current platform
     * - [cnnProvider]/[mlpProvider]: how to obtain inference modules for CNN/MLP
     */
    fun configure(
        resourceReader: ResourceReader,
        digitClassifierFactory: DigitClassifierFactory,
    ) {
        this.resourceReader = resourceReader
        this.injectedFactory = digitClassifierFactory
    }

    private fun ensureConfigured() {
        check(::resourceReader.isInitialized) { "ServiceLocator not configured: resourceReader is missing. Call ServiceLocator.configure(...) first." }
        // If factory is not injected, providers would be required, but we currently mandate injection.
    }
}
