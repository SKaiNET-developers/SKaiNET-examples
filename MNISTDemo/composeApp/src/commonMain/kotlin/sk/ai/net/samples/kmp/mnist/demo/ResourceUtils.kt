package sk.ai.net.samples.kmp.mnist.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import mnistdemo.composeapp.generated.resources.Res

/**
 * Utility functions for working with resources in a cross-platform way.
 */
object ResourceUtils {
    // Cache for loaded resources
    private val resourceCache = mutableMapOf<String, ByteArray>()

    // State flow to track loading status
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Initial)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    /**
     * Loads a resource file using Compose Resources and caches it.
     * This works across all platforms (Android, iOS, Desktop, WASM).
     *
     * @param resourcePath The path of the resource file relative to the composeResources directory.
     */
    suspend fun loadResource(resourcePath: String) {
        if (resourceCache.containsKey(resourcePath)) {
            _loadingState.value = LoadingState.Success
            return
        }

        _loadingState.value = LoadingState.Loading
        try {
            println("Attempting to read resource: $resourcePath")
            val bytes = Res.readBytes(resourcePath)
            println("Successfully read ${bytes.size} bytes from resource")
            resourceCache[resourcePath] = bytes
            _loadingState.value = LoadingState.Success
        } catch (e: Exception) {
            println("Exception in loadResource: ${e.toString()}")
            e.printStackTrace()
            _loadingState.value = LoadingState.Error(e.message ?: "Unknown error: ${e.toString()}")
        }
    }

    /**
     * Creates a Source from a previously loaded resource file.
     * Make sure to call loadResource() before using this function.
     *
     * @param resourcePath The path of the resource file relative to the composeResources directory.
     * @return A Source for the resource file, or null if the resource hasn't been loaded.
     */
    fun getSourceFromResource(resourcePath: String): Source? {
        val bytes = resourceCache[resourcePath]
        if (bytes == null) {
            println("Resource not found in cache: $resourcePath")
            return null
        }
        println("Creating Source from resource: $resourcePath (${bytes.size} bytes)")
        // Create a Buffer and write the bytes to it
        val buffer = Buffer()
        buffer.write(bytes)
        // Return the buffer as a Source
        return buffer
    }

    /**
     * Provides a function that returns a Source for a resource file.
     * This function will return null if the resource hasn't been loaded yet.
     *
     * @param resourcePath The path of the resource file relative to the composeResources directory.
     * @return A function that provides a Source for the resource file.
     */
    fun createSourceProvider(resourcePath: String): () -> Source = {
        getSourceFromResource(resourcePath) ?: Buffer()
    }
}

/**
 * State of resource loading.
 */
sealed interface LoadingState {
    data object Initial : LoadingState
    data object Loading : LoadingState
    data object Success : LoadingState
    data class Error(val message: String) : LoadingState
}
