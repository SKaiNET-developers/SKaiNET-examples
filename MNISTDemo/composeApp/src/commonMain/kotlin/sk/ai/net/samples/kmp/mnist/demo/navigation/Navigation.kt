package sk.ai.net.samples.kmp.mnist.demo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.io.Source

/**
 * Enum representing the different screens in the application
 */
enum class Screen {
    HOME,
    DRAWING,
    SETTINGS
}

/**
 * Class to manage navigation state in the application
 */
class NavigationState {
    var currentScreen by mutableStateOf(Screen.HOME)
    var handleSource: (() -> Source)? = null
}

/**
 * Composable function to provide navigation state to the application
 */
@Composable
fun rememberNavigationState(handleSource: () -> Source): NavigationState {
    val navigationState = remember { NavigationState() }
    navigationState.handleSource = handleSource
    return navigationState
}

/**
 * Composable function to navigate to a screen
 */
@Composable
fun NavigationHost(
    navigationState: NavigationState,
    homeScreen: @Composable () -> Unit,
    drawingScreen: @Composable (() -> Source) -> Unit,
    settingsScreen: @Composable () -> Unit
) {
    when (navigationState.currentScreen) {
        Screen.HOME -> homeScreen()
        Screen.DRAWING -> {
            navigationState.handleSource?.let { drawingScreen(it) }
        }
        Screen.SETTINGS -> settingsScreen()
    }
}