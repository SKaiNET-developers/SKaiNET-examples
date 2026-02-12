package sk.ainet.apps.kllama.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sk.ainet.apps.kllama.chat.navigation.ChatNavigationHost
import sk.ainet.ui.theme.SKaiNETTheme

/**
 * Main app composable with SKaiNET Design System theming.
 */
@Composable
fun App(
    darkTheme: Boolean = false
) {
    SKaiNETTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatNavigationHost()
        }
    }
}
