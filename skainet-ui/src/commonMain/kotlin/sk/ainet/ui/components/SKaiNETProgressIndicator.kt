package sk.ainet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.ainet.ui.theme.DarkPrimary
import sk.ainet.ui.theme.DarkPrimaryGlow

/**
 * SKaiNET-styled indeterminate circular progress indicator.
 *
 * Uses a Canvas-based custom drawing with the SKaiNET red accent color,
 * a glow arc, and smooth animated rotation.
 *
 * @param modifier Modifier for the composable
 * @param size The diameter of the progress indicator
 * @param strokeWidth The width of the progress arc stroke
 */
@Composable
fun SKaiNETProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        )
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size.minDimension
        val stroke = strokeWidth.toPx()
        val arcSize = canvasSize - stroke
        val topLeft = Offset(stroke / 2, stroke / 2)

        // Background track
        drawArc(
            color = DarkPrimary.copy(alpha = 0.15f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Glow arc (wider, semi-transparent)
        drawArc(
            color = DarkPrimaryGlow.copy(alpha = 0.3f),
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(arcSize, arcSize),
            style = Stroke(width = stroke * 2.5f, cap = StrokeCap.Round),
            blendMode = BlendMode.SrcOver
        )

        // Main arc
        drawArc(
            color = DarkPrimary,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
