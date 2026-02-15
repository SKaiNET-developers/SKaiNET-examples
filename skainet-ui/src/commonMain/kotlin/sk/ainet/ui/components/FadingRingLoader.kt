package sk.ainet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun indeterminateOrbitingFadingRingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    rotationDurationMillis: Int = 1200,
    ringCycleDurationMillis: Int = 1400,
    ringColor: Color = Color(0xFFC53A32),
    nodeColor: Color = Color(0xFF3F4543),
    edgeColor: Color = Color(0xFF3F4543),
    rightNodeColor: Color = Color(0xFFC53A32),
    minRingAlpha: Float = 0.15f,
    maxRingAlpha: Float = 1f,
    triangleAngleOffset: Float = 0f
) {
    val infinite = rememberInfiniteTransition(label = "SkaiNetOrbitingFadingRing")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = rotationDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "SkaiNetOrbitingAngle"
    )

    val ringAlpha by infinite.animateFloat(
        initialValue = minRingAlpha,
        targetValue = maxRingAlpha,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = ringCycleDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkaiNetRingAlpha"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = min(w, h)

        val center = Offset(w / 2f, h / 2f)

        val ringRadius = s * 0.40f
        val ringStroke = s * 0.111f  // Adjusted to match 2dp stroke width equivalent (2/18 ≈ 0.111)
        val edgeStroke = s * 0.065f  // Proportionally adjusted to maintain visual balance
        val nodeRadius = s * 0.115f

        // Fading full ring
        drawCircle(
            color = ringColor.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringStroke)
        )

        fun pointOnRing(angleDeg: Float): Offset {
            val a = (angleDeg.toDouble() * PI) / 180.0
            return Offset(
                x = center.x + ringRadius * cos(a).toFloat(),
                y = center.y + ringRadius * sin(a).toFloat()
            )
        }

        // Triangle rotates around the ring
        val rotation = angle + triangleAngleOffset
        val right = pointOnRing(0f + rotation)
        val topLeft = pointOnRing(120f + rotation)
        val bottomLeft = pointOnRing(-120f + rotation)

        // Edges
        drawLine(
            color = edgeColor,
            start = topLeft,
            end = bottomLeft,
            strokeWidth = edgeStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = edgeColor,
            start = topLeft,
            end = right,
            strokeWidth = edgeStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = edgeColor,
            start = bottomLeft,
            end = right,
            strokeWidth = edgeStroke,
            cap = StrokeCap.Round
        )

        // Nodes
        drawCircle(color = nodeColor, radius = nodeRadius, center = topLeft)
        drawCircle(color = nodeColor, radius = nodeRadius, center = bottomLeft)
        drawCircle(color = rightNodeColor, radius = nodeRadius, center = right)
    }
}

