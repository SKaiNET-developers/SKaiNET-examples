package sk.ainet.apps.kllama.chat.playground

import androidx.compose.runtime.Composable
import kllamademo.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sk.ainet.apps.kllama.chat.qwen.QwenModelSource

/**
 * Returns a loader for the GGUF bundled under composeResources at
 * [resourcePath]. Android copies it to app storage once and hands back the
 * file, so the weights can be memory-mapped; the other platforms read it into
 * memory.
 */
@Composable
internal expect fun rememberBundledModel(resourcePath: String): suspend () -> QwenModelSource

@OptIn(ExperimentalResourceApi::class)
internal suspend fun readBundledModelIntoMemory(resourcePath: String): QwenModelSource =
    QwenModelSource.InMemory(Res.readBytes(resourcePath))
