package sk.ai.net.samples.kmp.mnist.demo.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sk.ainet.clean.domain.model.ModelId
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the status of a model (pretrained vs locally trained).
 */
enum class ModelStatus {
    PRETRAINED,
    RETRAINED
}

/**
 * Simple in-memory app settings shared across screens.
 * Minimal solution to support model selection from Settings.
 */
object AppSettings {
    private val _selectedModelId = MutableStateFlow(ModelId.MLP_MNIST)
    val selectedModelId: StateFlow<ModelId> = _selectedModelId.asStateFlow()

    // Status for each model
    private val _modelStatuses = MutableStateFlow(
        mapOf(
            ModelId.CNN_MNIST to ModelStatus.PRETRAINED,
            ModelId.MLP_MNIST to ModelStatus.PRETRAINED
        )
    )
    val modelStatuses: StateFlow<Map<ModelId, ModelStatus>> = _modelStatuses.asStateFlow()

    fun setSelectedModel(modelId: ModelId) {
        _selectedModelId.value = modelId
    }

    fun setModelStatus(modelId: ModelId, status: ModelStatus) {
        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
            put(modelId, status)
        }
    }

    fun getModelStatus(modelId: ModelId): ModelStatus {
        return _modelStatuses.value[modelId] ?: ModelStatus.PRETRAINED
    }
}
