@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package sk.ainet.samples.layamoe.worker

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.MixtureClient
import sk.ainet.samples.layamoe.platform.logEvent

@JsFun("(url) => new Worker(url, { type: 'module' })")
private external fun createModuleWorker(url: String): JsAny

@JsFun("(worker, msg) => worker.postMessage(msg)")
private external fun workerPostMessage(worker: JsAny, msg: String)

@JsFun("(worker, onMessage, onError) => { worker.onmessage = (e) => onMessage(e.data); worker.onerror = (e) => onError(String(e && e.message ? e.message : e)); }")
private external fun installWorkerHandlers(worker: JsAny, onMessage: (String) -> Unit, onError: (String) -> Unit)

/**
 * The page's side of the mixture's Web Worker (see `webWorker/.../Main.kt` and [WorkerProtocol]):
 * the model and its gigabyte of weights live in the worker's own JS/Wasm context, the page only
 * exchanges strings with it, so it keeps repainting while a request runs.
 */
class WorkerMixtureClient(private val workerUrl: String) : MixtureClient {
    private var worker: JsAny? = null
    private var loaded: CompletableDeferred<String>? = null
    private var progress: ((String) -> Unit)? = null
    private var nextId = 0
    private var pending: Pair<Int, CompletableDeferred<AskResult>>? = null
    private val oneAtATime = Mutex()

    override suspend fun load(onProgress: (String) -> Unit): String {
        loaded?.let { return it.await() }
        val ready = CompletableDeferred<String>()
        loaded = ready
        progress = onProgress
        onProgress("starting the mixture worker…")
        val w = createModuleWorker(workerUrl)
        worker = w
        installWorkerHandlers(w, ::onMessage) { message ->
            logEvent("worker_error", "error" to message)
            ready.completeExceptionally(RuntimeException(message))
            pending?.second?.completeExceptionally(RuntimeException(message))
        }
        workerPostMessage(w, WorkerProtocol.LOAD)
        return ready.await()
    }

    override suspend fun ask(request: AskRequest): AskResult {
        val w = worker ?: error("load() first")
        loaded?.await()
        return oneAtATime.withLock {
            val id = nextId++
            val deferred = CompletableDeferred<AskResult>()
            pending = id to deferred
            workerPostMessage(w, WorkerProtocol.ask(id, request))
            try {
                deferred.await()
            } finally {
                if (pending?.first == id) pending = null
            }
        }
    }

    private fun onMessage(raw: String) {
        WorkerProtocol.parseReady(raw)?.let { tier ->
            logEvent("worker_ready", "tier" to tier)
            loaded?.complete(tier)
            return
        }
        WorkerProtocol.parseState(raw)?.let { (kind, text) ->
            if (kind == "error") loaded?.completeExceptionally(RuntimeException(text)) else progress?.invoke(text)
            return
        }
        WorkerProtocol.parseResult(raw)?.let { (id, result) ->
            val p = pending
            logEvent("worker_result", "id" to id, "expert" to result.expert, "ms" to result.totalMillis, "stale" to (p == null || p.first != id))
            if (p != null && p.first == id) p.second.complete(result)
            return
        }
        WorkerProtocol.parseError(raw)?.let { (id, message) ->
            if (id == null) loaded?.completeExceptionally(RuntimeException(message))
            else pending?.takeIf { it.first == id }?.second?.completeExceptionally(RuntimeException(message))
        }
    }
}
