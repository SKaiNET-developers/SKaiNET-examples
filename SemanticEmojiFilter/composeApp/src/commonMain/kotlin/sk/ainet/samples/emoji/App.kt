package sk.ainet.samples.emoji

import androidx.compose.runtime.Composable
import sk.ainet.samples.emoji.ui.SearchScreen
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel
import sk.ainet.ui.theme.SKaiNETTheme

@Composable
fun App(viewModel: EmojiSearchViewModel, skainetVersion: String) {
    SKaiNETTheme {
        SearchScreen(viewModel = viewModel, skainetVersion = skainetVersion)
    }
}
