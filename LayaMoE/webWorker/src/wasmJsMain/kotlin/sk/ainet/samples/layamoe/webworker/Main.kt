@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package sk.ainet.samples.layamoe.webworker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sk.ainet.samples.layamoe.engine.MixtureService
import sk.ainet.samples.layamoe.engine.WeightLane
import sk.ainet.samples.layamoe.model.WebModelFiles
import sk.ainet.samples.layamoe.worker.WorkerProtocol

/**
 * The mixture's Web Worker. The page's Kotlin/Wasm runs on the single JS thread, and a request is
 * seconds of straight compute even on the SIMD128 lane, so the model lives here: its own JS/Wasm
 * context, its own memory, and the page repaints between messages.
 *
 * Protocol: see [WorkerProtocol]. `LOAD` fetches (or takes from the browser cache) the base
 * checkpoint and the expert heads and builds them on the Q8_0 lane; every `ASK` answers with one
 * `RESULT`, in order. Loading progress is posted as `STATE` lines as it happens: a StateFlow
 * collector on this single thread could not run while the synchronous read and quantize work is
 * going on, so it would conflate minutes of progress into the first and the last line.
 */
@JsFun("(msg) => self.postMessage(msg)")
external fun postToMain(msg: String)

@JsFun("(handler) => { self.onmessage = (e) => handler(e.data); }")
external fun installOnMessage(handler: (String) -> Unit)

private val scope = CoroutineScope(Job())
private val service = MixtureService(WebModelFiles(), WeightLane.Q8_0)

fun main() {
    installOnMessage { raw -> scope.launch { handle(raw) } }
}

private suspend fun handle(raw: String) {
    when {
        raw == WorkerProtocol.LOAD -> try {
            val tier = service.load { line -> postToMain(WorkerProtocol.loading(line)) }
            postToMain(WorkerProtocol.ready(tier))
        } catch (t: Throwable) {
            postToMain(WorkerProtocol.failed(t.message ?: t::class.simpleName ?: "load failed"))
            postToMain(WorkerProtocol.error(null, t.message ?: t::class.simpleName ?: "load failed"))
        }
        else -> {
            val (id, request) = WorkerProtocol.parseAsk(raw) ?: return
            try {
                postToMain(WorkerProtocol.result(id, service.ask(request)))
            } catch (t: Throwable) {
                postToMain(WorkerProtocol.error(id, t.message ?: t::class.simpleName ?: "request failed"))
            }
        }
    }
}
