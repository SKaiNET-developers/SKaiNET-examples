package sk.ainet.apps.kllama.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sk.ainet.apps.kllama.chat.domain.model.ChatMessage
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.MessageRole
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository

/**
 * UI state for the chat screen.
 */
data class ChatUiState(
    val session: ChatSession = ChatSession(),
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val isGenerating: Boolean = false,
    val currentResponse: String = "",
    val statistics: InferenceStatistics = InferenceStatistics(),
    val modelMetadata: ModelMetadata? = null,
    val errorMessage: String? = null,
    val showModelPicker: Boolean = false
)

/**
 * ViewModel for the chat screen.
 * Manages chat state, model loading, and inference.
 */
class ChatViewModel(
    private val modelRepository: ModelRepository,
    private val inferenceEngineFactory: (LoadedModel?) -> InferenceEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentInferenceEngine: InferenceEngine? = null
    private var generationJob: Job? = null

    /**
     * Load a model from the given path.
     */
    fun loadModel(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModel = true, errorMessage = null) }

            when (val result = modelRepository.loadModel(path)) {
                is ModelLoadResult.Success -> {
                    currentInferenceEngine = inferenceEngineFactory(result.model)
                    _uiState.update {
                        it.copy(
                            isModelLoaded = true,
                            isLoadingModel = false,
                            modelMetadata = result.model.metadata,
                            showModelPicker = false
                        )
                    }
                }
                is ModelLoadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingModel = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Unload the current model.
     */
    fun unloadModel() {
        stopGeneration()
        modelRepository.unloadModel()
        currentInferenceEngine = null
        _uiState.update {
            it.copy(
                isModelLoaded = false,
                modelMetadata = null,
                session = ChatSession()
            )
        }
    }

    /**
     * Send a user message and generate a response.
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content.trim()
        )

        // Add user message to session
        _uiState.update {
            it.copy(
                session = it.session.addMessage(userMessage),
                errorMessage = null
            )
        }

        // Start generating response
        generateResponse()
    }

    /**
     * Generate an assistant response for the current session.
     */
    private fun generateResponse() {
        val engine = currentInferenceEngine ?: run {
            // Use demo mode if no model loaded
            currentInferenceEngine = inferenceEngineFactory(null)
            currentInferenceEngine
        }

        if (engine == null) {
            _uiState.update { it.copy(errorMessage = "No inference engine available") }
            return
        }

        // Add placeholder assistant message
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                session = it.session.addMessage(assistantMessage),
                isGenerating = true,
                currentResponse = "",
                statistics = InferenceStatistics()
            )
        }

        generationJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()

            try {
                engine.generate(
                    session = _uiState.value.session,
                    config = InferenceConfig()
                ).flowOn(Dispatchers.Default)
                    .conflate()
                    .collect { token ->
                    responseBuilder.append(token.token)

                    _uiState.update { state ->
                        state.copy(
                            session = state.session.updateLastMessage(
                                content = responseBuilder.toString(),
                                isStreaming = true,
                                tokenCount = token.statistics.tokensGenerated
                            ),
                            currentResponse = responseBuilder.toString(),
                            statistics = token.statistics
                        )
                    }
                }

                // Finalize the message
                _uiState.update { state ->
                    state.copy(
                        session = state.session.updateLastMessage(
                            content = responseBuilder.toString(),
                            isStreaming = false,
                            tokenCount = state.statistics.tokensGenerated
                        ),
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Generation error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Stop the current generation.
     */
    fun stopGeneration() {
        currentInferenceEngine?.stopGeneration()
        generationJob?.cancel()
        generationJob = null

        _uiState.update { state ->
            if (state.isGenerating) {
                state.copy(
                    session = state.session.updateLastMessage(
                        content = state.currentResponse,
                        isStreaming = false
                    ),
                    isGenerating = false
                )
            } else {
                state
            }
        }
    }

    /**
     * Clear the chat history.
     */
    fun clearChat() {
        stopGeneration()
        _uiState.update {
            it.copy(
                session = it.session.clearMessages(),
                currentResponse = "",
                statistics = InferenceStatistics()
            )
        }
    }

    /**
     * Toggle the model picker visibility.
     */
    fun toggleModelPicker() {
        _uiState.update { it.copy(showModelPicker = !it.showModelPicker) }
    }

    /**
     * Dismiss any error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
    }
}
