package sk.ainet.samples.visiondemo

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import sk.ainet.cartridge.profile.yolo.v1.YoloEngine
import sk.ainet.cartridge.profile.yolo.v1.YoloImage

/** A materialized pack the platform found (bundled assets unpacked, or loaded by hand). */
data class PackRef(val id: String, val path: String) {
    /** detection-yolov8-iree-vulkan-arm64 → "vulkan-arm64" */
    val short: String get() = id.removePrefix("detection-yolov8-iree-")
    val isAccelerated: Boolean get() = "vulkan" in id

    /** CPU, VULKAN, VK·320 — the backend pill text (variants shorten so three pills fit). */
    val pillLabel: String
        get() {
            val res = short.substringAfterLast('-').takeIf { it.all(Char::isDigit) }
            return when {
                res == null -> if (isAccelerated) "VULKAN" else "CPU"
                isAccelerated -> "VK·$res"
                else -> "CPU·$res"
            }
        }
}

/** Unpacks bundled packs if needed and lists what is available. Call off the UI thread. */
expect fun preparePacks(): List<PackRef>

/** Opens a pack as a `yolo/v1` engine on this platform's runner. Call off the UI thread. */
expect fun openEngine(pack: PackRef): YoloEngine

/** One upright camera frame: what to draw, and the same pixels already converted for the engine. */
class Frame(val image: ImageBitmap, val yolo: YoloImage)

/**
 * Feeds upright camera frames while [active]; emits on the UI thread. Rendering is the caller's —
 * the frames shown are the frames analyzed, so detection overlays map 1:1, and the [Frame] carries
 * the engine-ready pixels so the UI never re-reads the bitmap. No-op where the platform has no
 * camera (the picker path still works there).
 */
@Composable
expect fun CameraFeed(active: Boolean, front: Boolean, onFrame: (Frame) -> Unit)

/** A launcher for the platform image picker; invoke the returned function to open it. */
@Composable
expect fun rememberImagePicker(onPicked: (ImageBitmap) -> Unit): () -> Unit

/** Orbitron, the SKaiNET display face (falls back to the platform default). */
expect fun displayFontFamily(): FontFamily
