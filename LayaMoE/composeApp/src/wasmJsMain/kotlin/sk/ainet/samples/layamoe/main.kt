package sk.ainet.samples.layamoe

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import sk.ainet.samples.layamoe.vm.MoeViewModel
import sk.ainet.samples.layamoe.worker.WorkerMixtureClient

/**
 * The web build: the model lives in the :webWorker (Q8_0 lane, SIMD128 sidecar, the checkpoints in
 * the Cache API), started as a module worker from the file the build copies under moe-worker/.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        val viewModel = remember { MoeViewModel(WorkerMixtureClient("./moe-worker/LayaMoE-webWorker.mjs")) }
        App(viewModel = viewModel)
    }
}
