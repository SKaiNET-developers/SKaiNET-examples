package sk.ainet.apps.kllama.chat.playground

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kllamademo.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sk.ainet.apps.kllama.chat.qwen.QwenModelSource

private const val ASSET_URI_PREFIX = "file:///android_asset/"

@Composable
internal actual fun rememberBundledModel(resourcePath: String): suspend () -> QwenModelSource {
    val context = LocalContext.current.applicationContext
    return remember(resourcePath) { { QwenModelSource.OnDisk(copyBundledModelToFile(context, resourcePath).path) } }
}

/**
 * Copies the GGUF out of the APK into app storage and returns the file.
 *
 * Reading a model-sized asset into a ByteArray puts it on the ART heap, which
 * is capped per app (256 MB, 512 MB with largeHeap) however much RAM the
 * device has. A file can be memory-mapped instead, and mapped pages do not
 * count against that cap. An asset cannot be mapped in place: it is stored
 * compressed inside the APK.
 *
 * The copy is streamed, reused on later launches, and redone after an app
 * update. It costs the model's size in storage a second time.
 */
@OptIn(ExperimentalResourceApi::class)
private fun copyBundledModelToFile(context: Context, resourcePath: String): File {
    val uri = Res.getUri(resourcePath)
    check(uri.startsWith(ASSET_URI_PREFIX)) { "Bundled model is not an APK asset: $uri" }
    val assetPath = uri.removePrefix(ASSET_URI_PREFIX)

    val dir = File(context.filesDir, "models").apply { mkdirs() }
    val target = File(dir, File(resourcePath).name)
    val installedAt = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    if (target.isFile && target.length() > 0 && target.lastModified() >= installedAt) return target

    // Write to a sibling temp file and rename, so an interrupted copy is never mistaken
    // for a complete model.
    val partial = File(dir, target.name + ".partial")
    context.assets.open(assetPath).use { input ->
        partial.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 20) }
    }
    check(partial.renameTo(target)) { "Could not move the copied model into place: $target" }
    return target
}
