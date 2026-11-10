package com.calias.tv.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

/**
 * The loading indicator: a spinning arc. Pick the [diameter] for the place: big in the middle of the
 * screen, small inside a line or a badge.
 */
@Composable
fun Spinner(modifier: Modifier = Modifier, diameter: Dp = 48.dp, color: Color = LocalContentColor.current) {
    val turn = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = this.size.minDimension / 8
        // Read in the draw block, so only the drawing repeats, not the composition.
        drawArc(
            color = color,
            startAngle = turn.value,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
