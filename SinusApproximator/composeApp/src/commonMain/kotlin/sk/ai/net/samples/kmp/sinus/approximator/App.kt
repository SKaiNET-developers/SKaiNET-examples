package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.io.Source


@Composable
fun App() {
    MaterialTheme {
        SinusSliderScreen()
    }
}