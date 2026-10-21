package sk.ainet.samples.layamoe.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.MixtureClient
import sk.ainet.samples.layamoe.engine.Presets
import sk.ainet.samples.layamoe.engine.QuestionsJson
import sk.ainet.samples.layamoe.model.LayaMoeCheckpoints
import sk.ainet.samples.layamoe.platform.logEvent

sealed interface LoadState {
    /** Nothing loaded; the user starts the download with the button. */
    data object Idle : LoadState
    data class Loading(val message: String) : LoadState
    data class Ready(val kernelTier: String) : LoadState
    data class Failed(val message: String) : LoadState
}

/**
 * Drives the page: load on demand (it is a gigabyte), one request at a time, the last result.
 * Everything platform-specific is behind [MixtureClient], so the class runs under `runTest` with a fake.
 */
class MoeViewModel(private val client: MixtureClient) : ViewModel() {

    private val _status = MutableStateFlow<LoadState>(LoadState.Idle)
    val status: StateFlow<LoadState> = _status

    private val _result = MutableStateFlow<AskResult?>(null)
    val result: StateFlow<AskResult?> = _result

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val stateText = MutableStateFlow(Presets.ALL.first().state)
    val questionsText = MutableStateFlow(QuestionsJson.pretty(Presets.ALL.first().questions))
    /** `auto`, `general` or an expert name. */
    val expert = MutableStateFlow("auto")
    val compare = MutableStateFlow(false)
    /** Below this confidence the result line says the item would go to a person. */
    val actThreshold = MutableStateFlow(0.9)

    val downloadBytes: Long = LayaMoeCheckpoints.totalWeightBytes

    fun load() {
        if (_status.value !is LoadState.Idle && _status.value !is LoadState.Failed) return
        _status.value = LoadState.Loading("starting…")
        viewModelScope.launch {
            try {
                val tier = client.load { line -> _status.value = LoadState.Loading(line) }
                _status.value = LoadState.Ready(tier)
            } catch (t: Throwable) {
                logEvent("load_failed", "error" to (t.message ?: t::class.simpleName))
                _status.value = LoadState.Failed(t.message ?: t::class.simpleName ?: "load failed")
            }
        }
    }

    fun applyPreset(index: Int) {
        val p = Presets.ALL.getOrNull(index) ?: return
        stateText.value = QuestionsJson.prettyState(p.state)
        questionsText.value = QuestionsJson.pretty(p.questions)
        _error.value = null
    }

    fun ask() {
        if (_status.value !is LoadState.Ready || _busy.value) return
        val request = AskRequest(
            state = stateText.value,
            questions = questionsText.value,
            expert = expert.value.takeIf { it != "auto" },
            compare = compare.value,
        )
        // Validate on the UI side first, so a typo in the JSON is reported without a round trip.
        try {
            QuestionsJson.parseQuestions(request.questions)
        } catch (e: IllegalArgumentException) {
            _error.value = e.message
            return
        }
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _result.value = client.ask(request)
            } catch (t: Throwable) {
                logEvent("ask_failed", "error" to (t.message ?: t::class.simpleName))
                _error.value = t.message ?: t::class.simpleName ?: "request failed"
            } finally {
                _busy.value = false
            }
        }
    }
}
