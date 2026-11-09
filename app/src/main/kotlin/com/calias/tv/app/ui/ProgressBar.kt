package com.calias.tv.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one progress bar: [fraction] (0 to 1) of the way, gliding to each new value. In the theme accent;
 * [onVideo] draws it white on a faint white track instead, for what sits over the video.
 */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 3.dp, onVideo: Boolean = false) {
    val shown by animateFloatAsState(targetValue = fraction, label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(if (onVideo) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.4f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(shown)
                .fillMaxHeight()
                .background(if (onVideo) Color.White else Glass.colors.accent),
        )
    }
}
