package sk.ainet.samples.visiondemo

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.cartridge.profile.yolo.v1.Detection
import sk.ainet.ui.components.PoweredBySKaiNET
import sk.ainet.cartridge.profile.yolo.v1.YoloEngine
import sk.ainet.cartridge.profile.yolo.v1.YoloImage
import sk.ainet.cartridge.profile.yolo.v1.YoloResult

data class Shot(
    val id: Long,
    val image: ImageBitmap,
    val dets: List<Detection>,
    val ms: Int,
    val backend: String,
)

private enum class Tab { Live, Images }

@Composable
fun App(icons: AppIcons) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    // Material defaults tuned to the palette — only skainet-ui's components read them; the
    // app's own surfaces come from LocalDt.
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        surface = palette.surface,
        onSurfaceVariant = palette.mutedFg,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalDt provides palette) { AppContent(icons) }
    }
}

@Composable
private fun AppContent(icons: AppIcons) {
    val dt = LocalDt.current
    val display = remember { displayFontFamily() }
    val scope = rememberCoroutineScope()
    @Suppress("OPT_IN_USAGE")
    val serial = remember { Dispatchers.Default.limitedParallelism(1) }

    var packs by remember { mutableStateOf<List<PackRef>>(emptyList()) }
    var scanned by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<PackRef?>(null) }
    var engine by remember { mutableStateOf<YoloEngine?>(null) }
    var tab by remember { mutableStateOf(Tab.Live) }
    var paused by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf(false) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var dets by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var inferMs by remember { mutableStateOf(0) }
    var history by remember { mutableStateOf<List<Float>>(emptyList()) }
    var shots by remember { mutableStateOf<List<Shot>>(emptyList()) }
    var viewer by remember { mutableStateOf<Shot?>(null) }
    var flash by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    fun select(pack: PackRef) {
        if (pack == selected) return
        selected = pack
        history = emptyList()
        scope.launch {
            note = "opening ${pack.short}…"
            withContext(serial) {
                engine?.close()
                engine = null
                engine = openEngine(pack)
            }
            note = null
        }
    }

    LaunchedEffect(Unit) {
        val found = withContext(Dispatchers.Default) { preparePacks() }
        packs = found
        scanned = true
        (found.firstOrNull { it.isAccelerated } ?: found.firstOrNull())?.let(::select)
    }
    LaunchedEffect(flash) { if (flash) { kotlinx.coroutines.delay(160); flash = false } }

    fun inferLive(f: Frame) {
        val e = engine ?: return
        if (busy || paused) return
        busy = true
        scope.launch {
            try {
                val (result, ms) = withContext(serial) {
                    val yolo = f.yolo
                    val t0 = nowMillis()
                    val r = e.infer(yolo)
                    r to (nowMillis() - t0).toInt()
                }
                if (result is YoloResult.Detections) {
                    dets = result.boxes
                    inferMs = ms
                    history = (history + (1000f / ms.coerceAtLeast(1))).takeLast(40)
                }
            } catch (ex: Exception) {
                note = ex.message
            } finally {
                busy = false
            }
        }
    }

    val pickImage = rememberImagePicker { image ->
        val e = engine
        scope.launch {
            if (e == null) { note = "no pack open"; return@launch }
            note = "running ${selected?.short}…"
            val (result, ms) = withContext(serial) {
                val t0 = nowMillis()
                val r = e.infer(image.toYolo())
                r to (nowMillis() - t0).toInt()
            }
            note = null
            if (result is YoloResult.Detections) {
                val shot = Shot(nowMillis(), image, result.boxes, ms, selected?.short ?: "?")
                shots = listOf(shot) + shots
                viewer = shot
            }
        }
    }

    CameraFeed(active = tab == Tab.Live && packs.isNotEmpty(), front = front) { f ->
        frame = f.image
        inferLive(f)
    }

    Column(
        Modifier.fillMaxSize().background(dt.background).safeDrawingPadding(),
    ) {
        Header(display, icons, packs, selected, ::select)

        Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (tab == Tab.Live) {
                if (scanned && packs.isEmpty()) {
                    PackLessScreen(display)
                } else {
                    LiveView(display, frame, if (paused) emptyList() else dets, inferMs, history,
                        paused, flash, selected?.short ?: "–", note, dets.size)
                }
            } else {
                Gallery(display, icons, shots, onOpen = { viewer = it }, onPick = pickImage)
            }
        }

        // Controls
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBtn(if (paused) icons.play else icons.pause, enabled = tab == Tab.Live) { paused = !paused }
            CaptureButton(icons, uploadMode = tab == Tab.Images) {
                if (tab == Tab.Images) pickImage() else {
                    val img = frame ?: return@CaptureButton
                    shots = listOf(Shot(nowMillis(), img, dets, inferMs, selected?.short ?: "?")) + shots
                    flash = true
                }
            }
            RoundBtn(icons.flip, enabled = tab == Tab.Live) { front = !front }
        }

        // Tabs
        Row(Modifier.fillMaxWidth().background(dt.surface)) {
            TabBtn(display, icons.camera, "LIVE", tab == Tab.Live, Modifier.weight(1f)) { tab = Tab.Live }
            TabBtn(display, icons.images, "IMAGES · ${shots.size}", tab == Tab.Images, Modifier.weight(1f)) { tab = Tab.Images }
        }
        // The line every sample shows, from the shared design system; the version is generated
        // from the catalog (generateBuildInfo), so it is always the real one.
        PoweredBySKaiNET(SKAINET_VERSION, Modifier.background(dt.surface))
    }

    viewer?.let { shot ->
        Viewer(display, icons, shot) { viewer = null }
    }
}

// ---------- pieces ----------

/** What a fresh clone shows: no packs are bundled or downloadable — materialization is yours. */
@Composable
private fun PackLessScreen(display: FontFamily) {
    val dt = LocalDt.current
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
            .border(1.dp, dt.border, RoundedCornerShape(18.dp)).background(dt.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("NO PACKS INSTALLED", fontFamily = display, fontWeight = FontWeight.Bold,
            fontSize = 14.sp, letterSpacing = 3.sp, color = dt.foreground)
        Spacer(Modifier.height(12.dp))
        Text(
            "This demo runs a cartridge you materialize yourself — the YOLOv8 weights are " +
                "AGPL-3.0 and never ship with the app or its Maven artifacts (cartridge " +
                "$CARTRIDGE_VERSION is code only).",
            fontSize = 12.sp, color = dt.secondaryFg, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().background(dt.card, RoundedCornerShape(12.dp))
                .border(1.dp, dt.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (line in listOf(
                "git clone …/SKaiNET-cartridge-blueprints",
                "./gradlew materializeCartridge \\",
                "    -Pprofile=profiles/mine.json",
                "# then reinstall this app:",
                "./gradlew :androidApp:installDebug",
            )) Text(line, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = dt.mutedFg)
        }
        Spacer(Modifier.height(12.dp))
        Text("Full steps: VisionDemo/README.md", fontSize = 11.sp, color = dt.mutedFg)
    }
}

@Composable
private fun Header(
    display: FontFamily,
    icons: AppIcons,
    packs: List<PackRef>,
    selected: PackRef?,
    onSelect: (PackRef) -> Unit,
) {
    val dt = LocalDt.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(32.dp)
                    .shadow(10.dp, RoundedCornerShape(8.dp), spotColor = dt.primaryGlow, ambientColor = dt.primaryGlow)
                    .background(dt.primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icons.bolt, null, Modifier.size(16.dp), tint = Color.White) }
            Column {
                Text("SKaiNET", fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    letterSpacing = 3.sp, color = dt.foreground, maxLines = 1, softWrap = false)
                Text("YOLO · DETECT", fontFamily = display, fontSize = 9.sp, letterSpacing = 2.sp,
                    color = dt.mutedFg, maxLines = 1, softWrap = false)
            }
        }
        BackendSwitch(display, icons, packs, selected, onSelect)
    }
}

@Composable
private fun BackendSwitch(
    display: FontFamily,
    icons: AppIcons,
    packs: List<PackRef>,
    selected: PackRef?,
    onSelect: (PackRef) -> Unit,
) {
    val dt = LocalDt.current
    Row(
        Modifier.border(1.dp, dt.border, RoundedCornerShape(50)).background(dt.card, RoundedCornerShape(50)).padding(2.dp),
    ) {
        for (pack in packs) {
            val active = pack == selected
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (active) dt.primary else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSelect(pack) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(if (pack.isAccelerated) icons.activity else icons.cpu, null, Modifier.size(12.dp),
                    tint = if (active) Color.White else dt.mutedFg)
                Text(pack.pillLabel, fontFamily = display, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, softWrap = false,
                    color = if (active) Color.White else dt.mutedFg)
            }
        }
        if (packs.isEmpty()) {
            Text("NO PACKS", fontFamily = display, fontSize = 10.sp, letterSpacing = 1.sp,
                color = dt.mutedFg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

private fun Modifier.glass(dt: Palette, shape: RoundedCornerShape) =
    background(dt.scrim, shape).border(1.dp, dt.glassBorder, shape)

@Composable
private fun LiveView(
    display: FontFamily,
    frame: ImageBitmap?,
    dets: List<Detection>,
    inferMs: Int,
    history: List<Float>,
    paused: Boolean,
    flash: Boolean,
    backend: String,
    note: String?,
    objCount: Int,
) {
    val dt = LocalDt.current
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
            .border(1.dp, dt.border, RoundedCornerShape(18.dp)).background(dt.surface),
    ) {
        frame?.let {
            Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            BoxesOverlay(dets, it.width, it.height, crop = true, Modifier.fillMaxSize())
        }
        if (flash) Box(Modifier.fillMaxSize().background(dt.foreground.copy(alpha = 0.7f)))

        // HUD
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.glass(dt, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (inferMs > 0) (1000f / inferMs).format1() else "–",
                        fontFamily = display, fontWeight = FontWeight.Black, fontSize = 28.sp, color = dt.onGlass,
                    )
                    Text("FPS", fontFamily = display, fontSize = 10.sp, letterSpacing = 2.sp,
                        color = dt.primary, modifier = Modifier.padding(bottom = 4.dp))
                }
                Sparkline(history, Modifier.width(96.dp).height(22.dp))
            }
            Column(
                Modifier.glass(dt, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat(display, "INFER", if (inferMs > 0) "$inferMs ms" else "–")
                    Stat(display, "AVG", if (history.isNotEmpty()) history.average().toFloat().format1() + " fps" else "–")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat(display, "OBJS", "$objCount")
                    Stat(display, "MODE", if (paused) "paused" else "live")
                }
            }
        }

        // Status strip
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                .glass(dt, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val pulse by rememberInfiniteTransition().animateFloat(
                0.4f, 1f,
                InfiniteRepeatableSpec(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            )
            Box(
                Modifier.size(8.dp).alpha(if (paused) 1f else pulse)
                    .background(if (paused) dt.mutedFg else dt.primary, CircleShape),
            )
            Text(
                note ?: "$backend · $inferMs ms · ${summary(dets)}",
                color = dt.onGlass, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1,
            )
        }
    }
}

@Composable
private fun Stat(display: FontFamily, k: String, v: String) {
    val dt = LocalDt.current
    Column {
        Text(k, fontSize = 9.sp, letterSpacing = 2.sp, color = dt.mutedFg)
        Text(v, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = dt.onGlass)
    }
}

@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier) {
    val dt = LocalDt.current
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (values.max()) * 1.1f
        val min = (values.min()) * 0.9f
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i / 39f * size.width
            val y = size.height - (v - min) / span * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, dt.primary, style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** Detections in source-pixel coordinates, mapped by the same fit the image uses. */
@Composable
private fun BoxesOverlay(dets: List<Detection>, imgW: Int, imgH: Int, crop: Boolean, modifier: Modifier) {
    val dt = LocalDt.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        if (imgW == 0 || imgH == 0) return@Canvas
        val sx = size.width / imgW
        val sy = size.height / imgH
        val s = if (crop) maxOf(sx, sy) else minOf(sx, sy)
        val ox = (size.width - imgW * s) / 2f
        val oy = (size.height - imgH * s) / 2f
        dets.forEachIndexed { i, d ->
            val color = dt.boxColor(d.classId)
            val l = ox + d.box.x1 * s
            val t = oy + d.box.y1 * s
            val w = (d.box.x2 - d.box.x1) * s
            val h = (d.box.y2 - d.box.y1) * s
            drawRoundRect(
                color,
                topLeft = androidx.compose.ui.geometry.Offset(l, t),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
            val label = "${d.label ?: "class-${d.classId}"} ${d.score.format2()}"
            val layout = measurer.measure(
                label,
                TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            )
            val ly = (t - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
            val lx = l.coerceIn(0f, (size.width - layout.size.width - 8.dp.toPx()).coerceAtLeast(0f))
            drawRoundRect(
                color,
                topLeft = androidx.compose.ui.geometry.Offset(lx, ly),
                size = androidx.compose.ui.geometry.Size(layout.size.width + 8.dp.toPx(), layout.size.height + 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )
            drawText(layout, topLeft = androidx.compose.ui.geometry.Offset(lx + 4.dp.toPx(), ly + 1.dp.toPx()))
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val dt = LocalDt.current
    Box(
        Modifier.size(48.dp).alpha(if (enabled) 1f else 0.3f)
            .border(1.dp, dt.border, CircleShape).background(dt.card, CircleShape)
            .clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(20.dp), tint = dt.foreground) }
}

@Composable
private fun CaptureButton(icons: AppIcons, uploadMode: Boolean, onClick: () -> Unit) {
    val dt = LocalDt.current
    Box(
        Modifier.size(72.dp).border(4.dp, dt.foreground.copy(alpha = 0.8f), CircleShape)
            .padding(6.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize()
                .shadow(14.dp, CircleShape, spotColor = dt.primaryGlow, ambientColor = dt.primaryGlow)
                .background(dt.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (uploadMode) Icon(icons.upload, null, Modifier.size(24.dp), tint = Color.White) }
    }
}

@Composable
private fun TabBtn(
    display: FontFamily,
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val dt = LocalDt.current
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(horizontal = 40.dp).fillMaxWidth().height(2.dp)
                    .shadow(8.dp, RoundedCornerShape(1.dp), spotColor = dt.primaryGlow)
                    .background(dt.primary, RoundedCornerShape(1.dp)),
            )
        }
        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = if (active) dt.foreground else dt.mutedFg)
            Text(label, fontFamily = display, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, color = if (active) dt.foreground else dt.mutedFg)
        }
    }
}

@Composable
private fun Gallery(
    display: FontFamily,
    icons: AppIcons,
    shots: List<Shot>,
    onOpen: (Shot) -> Unit,
    onPick: () -> Unit,
) {
    val dt = LocalDt.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                Modifier.aspectRatio(0.75f).border(1.dp, dt.border, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)).clickable(onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icons.upload, null, Modifier.size(20.dp), tint = dt.mutedFg)
                Spacer(Modifier.height(4.dp))
                Text("Pick image", fontSize = 10.sp, color = dt.mutedFg)
            }
        }
        items(shots, key = { it.id }) { shot ->
            Box(
                Modifier.aspectRatio(0.75f).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, dt.border, RoundedCornerShape(12.dp))
                    .clickable { onOpen(shot) },
            ) {
                Image(shot.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text(
                    "${shot.dets.size} obj · ${shot.ms} ms",
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(4.dp)
                        .glass(dt, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = dt.onGlass,
                )
            }
        }
    }
}

@Composable
private fun Viewer(display: FontFamily, icons: AppIcons, shot: Shot, onClose: () -> Unit) {
    val dt = LocalDt.current
    Column(Modifier.fillMaxSize().background(dt.background).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DETECTIONS", fontFamily = display, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp, color = dt.foreground)
            Box(
                Modifier.size(36.dp).border(1.dp, dt.border, CircleShape).background(dt.card, CircleShape)
                    .clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(icons.close, null, Modifier.size(16.dp), tint = dt.foreground) }
        }
        Column(Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(shot.image.width.toFloat() / shot.image.height)
                    .clip(RoundedCornerShape(18.dp)).border(1.dp, dt.border, RoundedCornerShape(18.dp)),
            ) {
                Image(shot.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                BoxesOverlay(shot.dets, shot.image.width, shot.image.height, crop = false, Modifier.fillMaxSize())
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(display, "LATENCY", "${shot.ms} ms", Modifier.weight(1f))
                StatCard(display, "≈ FPS", (1000f / shot.ms.coerceAtLeast(1)).format1(), Modifier.weight(1f))
                StatCard(display, "BACKEND", if ("vulkan" in shot.backend) "VULKAN" else "CPU", Modifier.weight(1f))
            }
            Column(Modifier.padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                shot.dets.forEachIndexed { i, d ->
                    Row(
                        Modifier.fillMaxWidth().background(dt.card, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).background(dt.boxColor(d.classId), RoundedCornerShape(2.dp)))
                            Text(d.label ?: "class-${d.classId}", fontSize = 14.sp, color = dt.foreground)
                        }
                        Text("${(d.score * 100).toInt()}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = dt.mutedFg)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(display: FontFamily, k: String, v: String, modifier: Modifier) {
    val dt = LocalDt.current
    Column(
        modifier.border(1.dp, dt.border, RoundedCornerShape(12.dp))
            .background(dt.card, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(k, fontSize = 9.sp, letterSpacing = 2.sp, color = dt.mutedFg)
        Text(v, fontFamily = display, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = dt.foreground)
    }
}

// ---------- helpers ----------

/** The icon set, supplied per platform entry point (material-icons-extended on Android). */
data class AppIcons(
    val bolt: ImageVector,
    val cpu: ImageVector,
    val activity: ImageVector,
    val camera: ImageVector,
    val images: ImageVector,
    val flip: ImageVector,
    val upload: ImageVector,
    val close: ImageVector,
    val play: ImageVector,
    val pause: ImageVector,
)

private fun summary(dets: List<Detection>): String =
    if (dets.isEmpty()) "nothing" else dets.groupingBy { it.label ?: "class-${it.classId}" }
        .eachCount().entries.joinToString { "${it.value} ${it.key}" }

private val appOrigin = kotlin.time.TimeSource.Monotonic.markNow()
internal fun nowMillis(): Long = appOrigin.elapsedNow().inWholeMilliseconds

private fun Float.format1(): String = ((this * 10).toInt() / 10f).toString()
private fun Float.format2(): String {
    val h = (this * 100).toInt()
    return "${h / 100}.${(h % 100).toString().padStart(2, '0')}"
}

/** ARGB pixels → the profile's RGB888 [YoloImage]. */
internal fun ImageBitmap.toYolo(): YoloImage {
    val pm = toPixelMap()
    val buf = pm.buffer
    val rgb = ByteArray(width * height * 3)
    var o = 0
    for (y in 0 until height) {
        var i = pm.bufferOffset + y * pm.stride
        for (x in 0 until width) {
            val p = buf[i++]
            rgb[o++] = (p shr 16 and 0xFF).toByte()
            rgb[o++] = (p shr 8 and 0xFF).toByte()
            rgb[o++] = (p and 0xFF).toByte()
        }
    }
    return YoloImage(rgb, width, height)
}
