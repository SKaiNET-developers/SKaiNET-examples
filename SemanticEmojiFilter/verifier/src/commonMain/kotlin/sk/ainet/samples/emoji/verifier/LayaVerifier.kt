package sk.ainet.samples.emoji.verifier

import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import sk.ainet.decisions.DecisionAdapter
import sk.ainet.decisions.boolean
import sk.ainet.decisions.probabilityTrue
import sk.ainet.decisions.runtime.DecisionRuntime
import sk.ainet.decisions.runtime.DecisionSession
import sk.ainet.decisions.runtime.decide
import sk.ainet.decisions.runtime.openSession
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.platform.logEvent
import sk.ainet.samples.emoji.verify.Verdict
import sk.ainet.samples.emoji.verify.Verifier
import sk.ainet.samples.emoji.verify.VerifierState

/**
 * System 2: asks the Laya decision model, per candidate, whether the item fits the search.
 *
 * One request per item, in rank order, on a single-threaded dispatcher: the adapter runs one
 * encoder pass per question anyway, so this costs nothing extra and gives a cancellation point
 * after every item (a few seconds each on a laptop) instead of one long uncancellable call.
 * Verdicts are cached by (query, item), so retyping a query is free.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LayaVerifier(
    private val source: DecisionAdapterSource,
    private val schema: sk.ainet.decisions.dsl.DecisionSchema = EmojiMatchSchema.schema,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
    private val cacheSize: Int = 256,
) : Verifier {
    private val _state = MutableStateFlow<VerifierState>(VerifierState.Idle)
    override val state: StateFlow<VerifierState> = _state

    private val loadMutex = Mutex()
    private var session: DecisionSession? = null
    private var runtime: DecisionRuntime? = null
    private val cache = LinkedHashMap<String, Verdict>()

    override fun verify(query: String, items: List<CatalogItem>): Flow<Verdict> = flow {
        if (items.isEmpty()) return@flow
        val open = try {
            openSession()
        } catch (t: Throwable) {
            _state.value = VerifierState.Error(t.message ?: t::class.simpleName ?: "load failed")
            logEvent("verifier_load_failed", "error" to (t.message ?: t::class.simpleName))
            return@flow
        }
        try {
            items.forEachIndexed { index, item ->
                _state.value = VerifierState.Busy(index, items.size)
                val key = "$query\u0000${item.id}"
                val verdict = cache[key] ?: withContext(dispatcher) { decide(open, query, item) }.also { remember(key, it) }
                currentCoroutineContext().ensureActive()
                emit(verdict)
            }
        } finally {
            _state.value = VerifierState.Idle
        }
    }

    /** Loads the model now instead of on the first verification (the web worker does this on `LOAD`). */
    suspend fun preload() {
        openSession()
    }

    private suspend fun openSession(): DecisionSession = loadMutex.withLock {
        session?.let { return it }
        _state.value = VerifierState.Loading("loading ${source.modelId}…")
        val started = TimeSource.Monotonic.markNow()
        val adapter: DecisionAdapter = withContext(dispatcher) { source.open { _state.value = VerifierState.Loading(it) } }
        val rt = DecisionRuntime(listOf(adapter))
        val s = rt.openSession(schema)
        runtime = rt
        session = s
        logEvent("verifier_loaded", "model" to source.modelId, "loadMs" to started.elapsedNow().inWholeMilliseconds)
        _state.value = VerifierState.Idle
        s
    }

    private fun decide(session: DecisionSession, query: String, item: CatalogItem): Verdict {
        val started = TimeSource.Monotonic.markNow()
        val result = session.decide(schema, "match-${item.id}", EmojiMatchSchema.state(query, item))
        val ms = started.elapsedNow().inWholeMilliseconds
        val verdict = Verdict(
            itemId = item.id,
            probabilityTrue = result.probabilityTrue(EmojiMatchSchema.QUESTION),
            accepted = result.boolean(EmojiMatchSchema.QUESTION),
            millis = ms,
        )
        logEvent("verifier_decision", "item" to item.id, "pTrue" to verdict.probabilityTrue, "accepted" to verdict.accepted, "ms" to ms)
        return verdict
    }

    private fun remember(key: String, verdict: Verdict) {
        cache[key] = verdict
        if (cache.size > cacheSize) cache.remove(cache.keys.first())
    }

    fun close() {
        session?.close()
        runtime?.close()
        session = null
        runtime = null
    }
}

private suspend fun currentCoroutineContext() = kotlinx.coroutines.currentCoroutineContext()
