package sk.ainet.apps.kllama.chat.playground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import sk.ainet.apps.kllama.chat.qwen.QwenModelSource

@Composable
internal actual fun rememberBundledModel(resourcePath: String): suspend () -> QwenModelSource =
    remember(resourcePath) { { readBundledModelIntoMemory(resourcePath) } }
