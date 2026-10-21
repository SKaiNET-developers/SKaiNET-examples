package sk.ainet.samples.layamoe

import androidx.compose.runtime.Composable
import sk.ainet.samples.layamoe.ui.MoeScreen
import sk.ainet.samples.layamoe.vm.MoeViewModel
import sk.ainet.ui.theme.SKaiNETTheme

/** The whole app: one screen, the SKaiNET theme. [deviceNote] is a platform warning shown under the header (e.g. too little RAM). */
@Composable
fun App(viewModel: MoeViewModel, deviceNote: String? = null) {
    SKaiNETTheme {
        MoeScreen(viewModel = viewModel, deviceNote = deviceNote)
    }
}
