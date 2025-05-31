package sk.ai.net.samples.kmp.mnist.demo.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.io.Source
import sk.ai.net.samples.kmp.mnist.demo.DrawingScreen
import sk.ai.net.samples.kmp.mnist.demo.screens.HomeScreen
import sk.ai.net.samples.kmp.mnist.demo.screens.SettingsScreen
import sk.ai.net.samples.kmp.mnist.demo.ui.LocalHandleSource

/**
 * Home tab - displays app description and mode selection
 */
object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "Home"
        )

    @Composable
    override fun Content() {
        val tabNavigator = LocalTabNavigator.current
        val handleSource = LocalHandleSource.current

        HomeScreen(
            onGetStarted = {
                // Navigate to the Drawing tab when Get Started is clicked
                tabNavigator.current = DrawingTab(handleSource)
            }
        )
    }
}

/**
 * Drawing tab - displays the drawing screen for digit recognition
 */
class DrawingTab(private val handleSource: () -> Source) : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = "Draw"
        )

    @Composable
    override fun Content() {
        // Use the existing DrawingScreen
        DrawingScreen(handleSource)
    }
}

/**
 * Settings tab - displays app configuration options
 */
object SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Settings"
        )

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
