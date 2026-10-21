package sk.ainet.samples.layamoe

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import sk.ainet.samples.layamoe.engine.InProcessMixtureClient
import sk.ainet.samples.layamoe.engine.MixtureService
import sk.ainet.samples.layamoe.engine.WeightLane
import sk.ainet.samples.layamoe.model.FileModelFiles
import sk.ainet.samples.layamoe.platform.desktopModelDir
import sk.ainet.samples.layamoe.vm.MoeViewModel

/**
 * Desktop: the checkpoints live under `~/.skainet-examples/layamoe` (or `LAYAMOE_MODEL_DIR`), the
 * model runs in-process on the FP32 reference lane by default; `-Dlayamoe.lane=q8` (or the
 * `LAYAMOE_LANE=q8` environment variable) takes the packed Q8_0 lane.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SKaiNET LayaMoE", state = rememberWindowState(width = 1100.dp, height = 900.dp)) {
        val viewModel = remember {
            MoeViewModel(InProcessMixtureClient(MixtureService(FileModelFiles(desktopModelDir()), desktopLane())))
        }
        App(viewModel = viewModel)
    }
}

fun desktopLane(): WeightLane {
    val raw = (System.getProperty("layamoe.lane") ?: System.getenv("LAYAMOE_LANE") ?: "fp32").lowercase()
    return if (raw.startsWith("q8")) WeightLane.Q8_0 else WeightLane.FP32
}
