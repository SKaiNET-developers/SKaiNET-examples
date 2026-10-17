@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package sk.ainet.samples.emoji

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.platform.logEvent
import sk.ainet.samples.emoji.verify.DecideRequest
import sk.ainet.samples.emoji.verify.Verdict
import sk.ainet.samples.emoji.verify.Verifier
import sk.ainet.samples.emoji.verify.VerifierState
import sk.ainet.samples.emoji.verify.WorkerProtocol

@JsFun("(url) => new Worker(url, { type: 'module' })")
private external fun createModuleWorker(url: String): JsAny

@JsFun("(worker, msg) => worker.postMessage(msg)")
private external fun workerPostMessage(worker: JsAny, msg: String)

@JsFun("(worker, onMessage, onError) => { worker.onmessage = (e) => onMessage(e.data); worker.onerror = (e) => onError(String(e && e.message ? e.message : e)); }")
private external fun installWorkerHandlers(worker: JsAny, onMessage: (String) -> Unit, onError: (String) -> Unit)

/**
 * The page's side of the Laya Web Worker: the [Verifier] the view model drives, with the model
 * itself in the worker (see `webVerifier/.../Main.kt` and [WorkerProtocol]). Verdicts are cached by
 * (query, item) like the in-process verifier's; one request is in flight at a time, and a verdict
 * for a request the flow has abandoned (the query changed) is dropped by its id.
 */
class WorkerLayaVerifier(private val workerUrl: String, private val cacheSize: Int = 256) : Verifier {
    private val _state = MutableStateFlow<VerifierState>(VerifierState.OptIn("tap to load Laya (644 MB, runs in a Web Worker)"))
    override val state: StateFlow<VerifierState> = _state

    private val enabled = CompletableDeferred<Unit>()
    private var loaded: CompletableDeferred<Unit>? = null
    private var worker: JsAny? = null
    private var nextId = 0
    private var pending: Pair<Int, CompletableDeferred<Verdict>>? = null
    private val oneAtATime = Mutex()
    private val cache = LinkedHashMap<String, Verdict>()

    override fun enable() {
        if (enabled.isCompleted) return
        _state.value = VerifierState.Loading("starting the Laya worker…")
        val ready = CompletableDeferred<Unit>()
        loaded = ready
        val w = createModuleWorker(workerUrl)
        worker = w
        installWorkerHandlers(w, ::onMessage) { message ->
            logEvent("verifier_worker_error", "error" to message)
            _state.value = VerifierState.Error(message)
            ready.completeExceptionally(RuntimeException(message))
            pending?.second?.completeExceptionally(RuntimeException(message))
        }
        workerPostMessage(w, WorkerProtocol.LOAD)
        enabled.complete(Unit)
    }

    private fun onMessage(raw: String) {
        when {
            raw == WorkerProtocol.READY -> {
                logEvent("verifier_ready", "worker" to workerUrl)
                _state.value = VerifierState.Idle
                loaded?.complete(Unit)
            }
            raw.startsWith("STATE:") -> WorkerProtocol.parseState(raw)?.let {
                logEvent("verifier_state", "state" to it)
                _state.value = it
            }
            raw.startsWith("VERDICT:") -> WorkerProtocol.parseVerdict(raw)?.let { (id, verdict) ->
                val p = pending
                logEvent("verifier_verdict", "item" to verdict.itemId, "pTrue" to verdict.probabilityTrue, "accepted" to verdict.accepted, "ms" to verdict.millis, "stale" to (p == null || p.first != id))
                if (p != null && p.first == id) p.second.complete(verdict)
            }
            raw.startsWith("ERROR:") -> WorkerProtocol.parseError(raw)?.let { (id, message) ->
                _state.value = VerifierState.Error(message)
                if (id == null) loaded?.completeExceptionally(RuntimeException(message))
                else pending?.takeIf { it.first == id }?.second?.completeExceptionally(RuntimeException(message))
            }
        }
    }

    override fun verify(query: String, items: List<CatalogItem>): Flow<Verdict> = flow {
        if (items.isEmpty()) return@flow
        enabled.await()
        try {
            loaded?.await()
        } catch (t: Throwable) {
            return@flow
        }
        val w = worker ?: return@flow
        try {
            items.forEachIndexed { index, item ->
                _state.value = VerifierState.Busy(index, items.size)
                val key = "$query\u0000${item.id}"
                val verdict = cache[key] ?: oneAtATime.withLock {
                    val id = nextId++
                    val deferred = CompletableDeferred<Verdict>()
                    pending = id to deferred
                    workerPostMessage(w, WorkerProtocol.decide(id, DecideRequest(query, item)))
                    try {
                        deferred.await()
                    } finally {
                        if (pending?.first == id) pending = null
                    }
                }.also { remember(key, it) }
                emit(verdict)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            _state.value = VerifierState.Error(t.message ?: t::class.simpleName ?: "decision failed")
            return@flow
        } finally {
            if (_state.value is VerifierState.Busy) _state.value = VerifierState.Idle
        }
    }

    private fun remember(key: String, verdict: Verdict) {
        cache[key] = verdict
        if (cache.size > cacheSize) cache.remove(cache.keys.first())
    }
}
