package sk.ainet.samples.tda.app.plots

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import sk.ainet.samples.tda.PersistenceDiagram
import sk.ainet.samples.tda.PersistencePair
import sk.ainet.samples.tda.PointCloud
import sk.ainet.samples.tda.format
import sk.ainet.ui.plot.AxisConfig
import sk.ainet.ui.plot.GridConfig
import sk.ainet.ui.plot.Plot
import sk.ainet.ui.plot.PlotBounds
import sk.ainet.ui.plot.PlotPadding
import kotlin.math.sqrt

internal val H0Color = Color(0xFFEF4444)
internal val H1Color = Color(0xFF3B82F6)

private val scatterPadding = PlotPadding(left = 44f, right = 12f, top = 10f, bottom = 26f)

/** Square data window around the cloud, so equal pixel scaling on a square canvas keeps shapes. */
private fun scatterBounds(points: PointCloud): PlotBounds {
    val b = points.bounds
    val half = maxOf(b.rangeX, b.rangeY) * 0.54f
    val cx = (b.minX + b.maxX) / 2f; val cy = (b.minY + b.maxY) / 2f
    return PlotBounds(cx - half, cx + half, cy - half, cy + half)
}

/**
 * The point cloud with every edge of length ≤ ε. Pinch/scroll to zoom, drag to pan, tap a
 * point to select it. Built on skainet-ui's [Plot], which supplies axes and the data→pixel map.
 */
@Composable
internal fun ScatterPlot(
    points: PointCloud,
    edges: IntArray,
    selected: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounds = remember(points) { scatterBounds(points) }
    val pointColor = MaterialTheme.colorScheme.primary
    val edgeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val selectedColor = MaterialTheme.colorScheme.tertiary

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(0.5f, 12f)
        offset += pan
    }

    Box(
        modifier
            .pointerInput(points, bounds) {
                detectTapGestures(
                    onDoubleTap = { scale = 1f; offset = Offset.Zero },
                    onTap = { tap ->
                        // Same mapping as PlotScope.dataToPixel, then the graphicsLayer transform
                        // (scale about the centre, then translate).
                        val w = size.width.toFloat(); val h = size.height.toFloat()
                        val left = scatterPadding.left; val top = scatterPadding.top
                        val pw = w - left - scatterPadding.right; val ph = h - top - scatterPadding.bottom
                        val cx = w / 2f; val cy = h / 2f
                        var best = -1; var bestDist = 28f * 28f
                        for (i in 0 until points.size) {
                            val px = left + (points.x(i) - bounds.xMin) / bounds.xRange * pw
                            val py = top + ph - (points.y(i) - bounds.yMin) / bounds.yRange * ph
                            val tx = (px - cx) * scale + cx + offset.x
                            val ty = (py - cy) * scale + cy + offset.y
                            val dx = tap.x - tx; val dy = tap.y - ty
                            val d2 = dx * dx + dy * dy
                            if (d2 < bestDist) { bestDist = d2; best = i }
                        }
                        onPointSelected(if (best >= 0) best else null)
                    },
                )
            }
            .transformable(transform),
    ) {
        Plot(
            bounds = bounds,
            modifier = Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y,
            ),
            padding = scatterPadding,
            xAxis = AxisConfig(tickCount = 3, labelFormatter = { it.format(1) }),
            yAxis = AxisConfig(tickCount = 3, labelFormatter = { it.format(1) }),
            grid = GridConfig(),
        ) {
            val stroke = 1.5f / scale
            var e = 0
            while (e + 1 < edges.size) {
                val i = edges[e]; val j = edges[e + 1]
                drawScope.drawLine(
                    edgeColor,
                    Offset(dataToPixelX(points.x(i)), dataToPixelY(points.y(i))),
                    Offset(dataToPixelX(points.x(j)), dataToPixelY(points.y(j))),
                    strokeWidth = stroke,
                )
                e += 2
            }
            val r = 4.5f / sqrt(scale)
            for (i in 0 until points.size) {
                drawScope.drawCircle(pointColor, r, Offset(dataToPixelX(points.x(i)), dataToPixelY(points.y(i))))
            }
            selected?.let { i ->
                drawScope.drawCircle(selectedColor, r * 2.2f, Offset(dataToPixelX(points.x(i)), dataToPixelY(points.y(i))))
            }
        }
    }
}

/** Bars shown in the barcode: all H₁ bars with positive length, plus the longest H₀ bars. */
private fun barcodeBars(diagram: PersistenceDiagram, maxH0: Int = 60): List<PersistencePair> {
    val h0 = diagram.bars(0, includeZero = true).take(maxH0).sortedBy { it.death }
    val h1 = diagram.bars(1).sortedBy { it.birth }
    return h0 + h1
}

/** Horizontal bars `[birth, death)`, H₀ then H₁, with a vertical marker at the current ε. */
@Composable
internal fun BarcodePlot(diagram: PersistenceDiagram, epsilon: Float, maxEpsilon: Float, modifier: Modifier = Modifier) {
    val bars = remember(diagram) { barcodeBars(diagram) }
    val marker = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val bounds = PlotBounds(0f, maxEpsilon.coerceAtLeast(1e-3f), 0f, bars.size.coerceAtLeast(1).toFloat())

    Plot(
        bounds = bounds,
        modifier = modifier,
        padding = PlotPadding(left = 44f, right = 12f, top = 8f, bottom = 26f),
        xAxis = AxisConfig(tickCount = 4, labelFormatter = { it.format(2) }),
        yAxis = AxisConfig(show = false),
        grid = GridConfig(showHorizontal = false),
    ) {
        val rowH = plotArea.height / bars.size.coerceAtLeast(1)
        val stroke = (rowH * 0.65f).coerceIn(1.5f, 10f)
        bars.forEachIndexed { idx, bar ->
            val y = plotArea.top + rowH * (idx + 0.5f)
            val x0 = dataToPixelX(bar.birth)
            val end = if (bar.isInfinite) maxEpsilon else bar.death
            val x1 = dataToPixelX(end.coerceAtMost(maxEpsilon))
            val color = if (bar.dim == 0) H0Color else H1Color
            val alive = epsilon >= bar.birth && epsilon < bar.death
            drawScope.drawLine(color.copy(alpha = if (alive) 1f else 0.35f), Offset(x0, y), Offset(x1, y), stroke)
            if (bar.isInfinite) {
                drawScope.drawLine(color, Offset(x1 - 6f, y - 5f), Offset(x1, y), 2f)
                drawScope.drawLine(color, Offset(x1 - 6f, y + 5f), Offset(x1, y), 2f)
            }
        }
        val xe = dataToPixelX(epsilon)
        drawScope.drawLine(
            marker, Offset(xe, plotArea.top), Offset(xe, plotArea.bottom), 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
    }
}

/** Birth/death scatter above the diagonal; infinite bars sit on the top edge. */
@Composable
internal fun PersistenceDiagramPlot(diagram: PersistenceDiagram, epsilon: Float, maxEpsilon: Float, modifier: Modifier = Modifier) {
    val top = maxEpsilon.coerceAtLeast(1e-3f) * 1.05f
    val bounds = PlotBounds(0f, top, 0f, top)
    val diagonal = MaterialTheme.colorScheme.onSurfaceVariant
    val marker = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val pairs = remember(diagram) { diagram.pairs.filter { it.persistence > 0f } }

    Plot(
        bounds = bounds,
        modifier = modifier,
        padding = PlotPadding(left = 44f, right = 12f, top = 8f, bottom = 26f),
        xAxis = AxisConfig(tickCount = 4, labelFormatter = { it.format(2) }),
        yAxis = AxisConfig(tickCount = 4, labelFormatter = { it.format(2) }),
        grid = GridConfig(),
    ) {
        drawScope.drawLine(diagonal, Offset(dataToPixelX(0f), dataToPixelY(0f)), Offset(dataToPixelX(top), dataToPixelY(top)), 1.5f)
        // Everything in the shaded square [0,ε]×[ε,∞) is alive at ε: born before, dies after.
        val xe = dataToPixelX(epsilon); val ye = dataToPixelY(epsilon)
        drawScope.drawLine(marker, Offset(xe, plotArea.top), Offset(xe, ye), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        drawScope.drawLine(marker, Offset(plotArea.left, ye), Offset(xe, ye), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        for (p in pairs) {
            val death = if (p.isInfinite) top else p.death
            val color = if (p.dim == 0) H0Color else H1Color
            val alive = epsilon >= p.birth && epsilon < p.death
            drawScope.drawCircle(color.copy(alpha = if (alive) 1f else 0.4f), if (p.dim == 1) 6f else 4f, Offset(dataToPixelX(p.birth), dataToPixelY(death)))
        }
    }
}
