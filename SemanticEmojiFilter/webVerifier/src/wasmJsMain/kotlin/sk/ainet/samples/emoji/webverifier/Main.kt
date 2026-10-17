@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package sk.ainet.samples.emoji.webverifier

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import sk.ainet.samples.emoji.verifier.LayaVerifier
import sk.ainet.samples.emoji.verifier.LayaWebSource
import sk.ainet.samples.emoji.verify.VerifierState
import sk.ainet.samples.emoji.verify.WorkerProtocol

/**
 * The Laya Web Worker. The page's Kotlin/Wasm runs on the single JS thread, and a Laya decision
 * is a second or more of straight compute even on the SIMD128 lane, so the model lives here:
 * its own JS/Wasm context, its own memory, and the page repaints between messages.
 *
 * Protocol: see [WorkerProtocol]. `LOAD` fetches (or takes from the browser cache) and builds
 * the Q8_0 agent; every `DECIDE` answers with one `VERDICT`, in order. Loading progress is
 * forwarded as `STATE` messages; the main thread keeps its own busy counter.
 */
@JsFun("(msg) => self.postMessage(msg)")
external fun postToMain(msg: String)

@JsFun("(handler) => { self.onmessage = (e) => handler(e.data); }")
external fun installOnMessage(handler: (String) -> Unit)

private val scope = CoroutineScope(Job())
private val verifier = LayaVerifier(LayaWebSource(report = { postToMain(WorkerProtocol.loading(it)) }))

fun main() {
    scope.launch {
        verifier.state.collect { state ->
            // Progress lines go straight from LayaWebSource; this flow cannot keep up with them.
            if (state is VerifierState.Error) postToMain(WorkerProtocol.failed(state.message))
        }
    }
    installOnMessage { raw -> scope.launch { handle(raw) } }
}

private suspend fun handle(raw: String) {
    when {
        raw == WorkerProtocol.LOAD -> try {
            verifier.preload()
            postToMain(WorkerProtocol.READY)
        } catch (t: Throwable) {
            postToMain(WorkerProtocol.error(null, t.message ?: t::class.simpleName ?: "load failed"))
        }
        else -> {
            val (id, request) = WorkerProtocol.parseDecide(raw) ?: return
            try {
                val verdict = verifier.verify(request.query, listOf(request.item)).firstOrNull()
                if (verdict != null) postToMain(WorkerProtocol.verdict(id, verdict))
                else postToMain(WorkerProtocol.error(id, (verifier.state.value as? VerifierState.Error)?.message ?: "no verdict"))
            } catch (t: Throwable) {
                postToMain(WorkerProtocol.error(id, t.message ?: t::class.simpleName ?: "decision failed"))
            }
        }
    }
}
