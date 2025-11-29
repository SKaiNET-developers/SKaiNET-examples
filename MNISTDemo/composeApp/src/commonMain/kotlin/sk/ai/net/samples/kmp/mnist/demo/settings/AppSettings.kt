package sk.ai.net.samples.kmp.mnist.demo.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sk.ainet.clean.domain.model.ModelId

/**
 * Simple in-memory app settings shared across screens.
 * Minimal solution to support model selection from Settings.
 */
object AppSettings {
    private val _selectedModelId = MutableStateFlow(ModelId.CNN_MNIST)
    val selectedModelId: StateFlow<ModelId> = _selectedModelId

    fun setSelectedModel(modelId: ModelId) {
        _selectedModelId.value = modelId
    }
}
