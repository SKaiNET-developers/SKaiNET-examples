package sk.ai.net.samples.kmp.mnist.demo.ui

import androidx.compose.runtime.compositionLocalOf
import kotlinx.io.Source

/**
 * CompositionLocal for the handleSource function
 * This allows us to access the handleSource function from any composable in the app
 */
val LocalHandleSource = compositionLocalOf<() -> Source> { 
    // Default implementation that will throw an error if used without a provider
    { error("No handleSource provided") } 
}