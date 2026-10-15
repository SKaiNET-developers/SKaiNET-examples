package sk.ainet.samples.emoji.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.RankedItem
import sk.ainet.samples.emoji.domain.Tier
import sk.ainet.samples.emoji.engine.CatalogIndex
import sk.ainet.samples.emoji.engine.EmbeddingEngine
import sk.ainet.samples.emoji.platform.logEvent
import sk.ainet.samples.emoji.verify.NoopVerifier
import sk.ainet.samples.emoji.verify.Verdict
import sk.ainet.samples.emoji.verify.Verifier

sealed interface LoadState {
    data object Idle : LoadState
    data class Loading(val message: String) : LoadState
    data object Ready : LoadState
    data class Failed(val message: String) : LoadState
}

/** Timings the diagnostics row shows; all milliseconds. */
data class SearchStats(
    val modelLoadMs: Long = 0,
    val indexBuildMs: Long = 0,
    val lastQueryMs: Long = 0,
    val p50QueryMs: Long = 0,
    val queries: Int = 0,
)

/**
 * Drives model loading, the per-keystroke ranking and the optional second-stage verifier.
 * Everything platform-specific (where the checkpoint comes from, how the index is obtained,
 * which verifier exists) is injected, so the class runs under `runTest` with fakes.
 *
 * Ranking: the query flow is debounced by [debounceMs]; `mapLatest` drops a sweep when a newer
 * query arrives. Verification: once the ranking has been stable for [stableMs], the top
 * [verifierTopK] items go to the [verifier], whose verdicts stream in one by one and are
 * cleared the moment the query changes.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EmojiSearchViewModel(
    private val loadEngine: suspend (onProgress: (String) -> Unit) -> EmbeddingEngine,
    private val loadIndex: suspend (engine: EmbeddingEngine, onProgress: (String) -> Unit) -> CatalogIndex,
    private val calibration: Calibration,
    val verifier: Verifier = NoopVerifier(),
    private val embedDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val debounceMs: Long = 120,
    private val stableMs: Long = 500,
    private val verifierTopK: Int = 6,
) : ViewModel() {

    val query = MutableStateFlow("")

    private val _status = MutableStateFlow<LoadState>(LoadState.Idle)
    val status: StateFlow<LoadState> = _status

    private val _ranked = MutableStateFlow<List<RankedItem>>(emptyList())
    val ranked: StateFlow<List<RankedItem>> = _ranked

    private val _verdicts = MutableStateFlow<Map<String, Verdict>>(emptyMap())
    val verdicts: StateFlow<Map<String, Verdict>> = _verdicts

    private val _stats = MutableStateFlow(SearchStats())
    val stats: StateFlow<SearchStats> = _stats

    private val queryTimings = ArrayList<Long>()
    private var engine: EmbeddingEngine? = null
    private var index: CatalogIndex? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            _status.value = LoadState.Loading("Loading model…")
            val started = TimeSource.Monotonic.markNow()
            val e = withContext(embedDispatcher) { loadEngine { _status.value = LoadState.Loading(it) } }
            val loadMs = started.elapsedNow().inWholeMilliseconds
            val indexStart = TimeSource.Monotonic.markNow()
            val idx = withContext(embedDispatcher) { loadIndex(e) { _status.value = LoadState.Loading(it) } }
            val indexMs = indexStart.elapsedNow().inWholeMilliseconds
            engine = e
            index = idx
            _stats.update { it.copy(modelLoadMs = loadMs, indexBuildMs = indexMs) }
            _ranked.value = idx.unranked()
            _status.value = LoadState.Ready
            logEvent("model_ready", "loadMs" to loadMs, "indexMs" to indexMs, "items" to idx.items.size, "dims" to idx.dimensions)
            startSearchLoop()
            startVerifierLoop()
        } catch (t: Throwable) {
            logEvent("model_failed", "error" to (t.message ?: t::class.simpleName))
            _status.value = LoadState.Failed(t.message ?: t::class.simpleName ?: "unknown error")
        }
    }

    private fun startSearchLoop() {
        viewModelScope.launch {
            query
                .debounce(debounceMs)
                .distinctUntilChanged()
                .mapLatest { q -> rank(q) }
                .collect { result ->
                    _verdicts.value = emptyMap()
                    _ranked.value = result
                }
        }
    }

    private fun startVerifierLoop() {
        viewModelScope.launch {
            _ranked
                .debounce(stableMs)
                .mapLatest { ranked -> verify(ranked) }
                .collect()
        }
    }

    private suspend fun rank(q: String): List<RankedItem> {
        val idx = index ?: return emptyList()
        val e = engine ?: return emptyList()
        if (q.isBlank()) return idx.unranked()
        return withContext(embedDispatcher) {
            val started = TimeSource.Monotonic.markNow()
            val vector = e.embedQuery(q.trim())
            val ranked = idx.rank(vector, calibration)
            val ms = started.elapsedNow().inWholeMilliseconds
            recordTiming(ms)
            ranked
        }
    }

    private suspend fun verify(ranked: List<RankedItem>) {
        val q = query.value.trim()
        if (q.isBlank() || ranked.isEmpty()) return
        val top = ranked.filter { it.tier == Tier.TOP }.take(verifierTopK).ifEmpty { ranked.take(3) }
        verifier.verify(q, top.map { it.item }).collect { verdict ->
            _verdicts.update { it + (verdict.itemId to verdict) }
        }
    }

    private fun recordTiming(ms: Long) {
        queryTimings += ms
        if (queryTimings.size > 50) queryTimings.removeAt(0)
        val sorted = queryTimings.sorted()
        _stats.update { it.copy(lastQueryMs = ms, p50QueryMs = sorted[sorted.size / 2], queries = it.queries + 1) }
    }
}
