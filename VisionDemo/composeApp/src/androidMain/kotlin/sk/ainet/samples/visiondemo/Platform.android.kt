package sk.ainet.samples.visiondemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.cartridge.detection.yolov8.PackDir
import sk.ainet.cartridge.detection.yolov8.YoloV8Cartridge
import sk.ainet.cartridge.profile.yolo.v1.YoloEngine
import sk.ainet.cartridge.profile.yolo.v1.YoloImage
import java.io.File
import java.util.concurrent.Executors

/** Set by [MainActivity] before composition. */
@SuppressLint("StaticFieldLeak")
internal lateinit var appContext: Context

actual fun preparePacks(): List<PackRef> {
    // Bundled packs (assets/packs/<pack_dir>/…) unpack into files/packs once; the IREE runtime
    // opens files by path, so assets must reach the file system first. adb-loaded packs land in
    // the same place (push to /data/local/tmp, then run-as cp — debug builds).
    try {
        for (name in appContext.assets.list("packs").orEmpty()) {
            YoloV8Cartridge.packFromAssets(appContext, "packs/$name")
        }
    } catch (_: Exception) {
        // no bundled packs
    }
    return File(appContext.filesDir, "packs")
        .listFiles { f: File -> File(f, "descriptor.json").isFile }
        .orEmpty()
        .sortedBy { it.name }
        .map { PackRef(it.name, it.absolutePath) }
}

actual fun openEngine(pack: PackRef): YoloEngine = YoloV8Cartridge.open(PackDir(pack.path))

@Composable
actual fun CameraFeed(active: Boolean, front: Boolean, onFrame: (Frame) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentOnFrame by rememberUpdatedState(onFrame)

    DisposableEffect(active, front) {
        if (!active) return@DisposableEffect onDispose { }
        val executor = Executors.newSingleThreadExecutor()
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val p = try { future.get() } catch (_: Exception) { return@addListener }
            provider = p
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val rotation = proxy.imageInfo.rotationDegrees
                val bitmap = proxy.toBitmap()
                proxy.close()
                val matrix = Matrix().apply {
                    if (rotation != 0) postRotate(rotation.toFloat())
                    if (front) postScale(-1f, 1f) // mirror, like a selfie preview
                }
                val upright =
                    if (rotation == 0 && !front) bitmap
                    else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                // Convert for the engine here, on the camera thread — the UI never re-reads pixels.
                val frame = Frame(upright.asImageBitmap(), upright.toYolo())
                scope.launch(Dispatchers.Main) { currentOnFrame(frame) }
            }
            try {
                p.unbindAll()
                p.bindToLifecycle(
                    lifecycleOwner,
                    if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                    analysis,
                )
            } catch (_: Exception) {
                // no camera / permission denied — picker path still works
            }
        }, context.mainExecutor)
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ImageBitmap) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { decodeBounded(context, uri) }
            if (bmp != null) onPicked(bmp.asImageBitmap())
        }
    }
    return { launcher.launch("image/*") }
}

/** Bounded decode: big camera photos add nothing past the letterbox, only conversion time. */
private fun decodeBounded(context: Context, uri: Uri, maxEdge: Int = 2048): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge / 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
} catch (_: Exception) {
    null
}

/** ARGB bitmap → the profile's RGB888 [YoloImage], straight from the pixel array. */
private fun Bitmap.toYolo(): YoloImage {
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, width, height)
    val rgb = ByteArray(argb.size * 3)
    var o = 0
    for (p in argb) {
        rgb[o++] = (p shr 16 and 0xFF).toByte()
        rgb[o++] = (p shr 8 and 0xFF).toByte()
        rgb[o++] = (p and 0xFF).toByte()
    }
    return YoloImage(rgb, width, height)
}

actual fun displayFontFamily(): FontFamily = try {
    FontFamily(Typeface(android.graphics.Typeface.createFromAsset(appContext.assets, "fonts/Orbitron.ttf")))
} catch (_: Exception) {
    FontFamily.Default
}
