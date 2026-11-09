package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ui.Edge
import com.calias.tv.core.seek.Seek

/**
 * The bottom bar for movies and episodes: title, a progress bar that glides to each new position,
 * and elapsed / total time. It slides up and fades in, and slides down and fades out.
 * Colors are fixed (white on a dark scrim) because it sits on top of video, in either theme.
 */
@Composable
fun VodOverlay(
    visible: Boolean,
    title: String,
    positionMs: () -> Long,
    durationMs: () -> Long,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = Edge.X, vertical = Edge.Y),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.titleLarge)

            // Read here, not in the caller, so each progress tick recomposes only this bar.
            val position = positionMs()
            val duration = durationMs()
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            val animatedFraction by animateFloatAsState(targetValue = fraction, label = "progress")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(6.dp)
                        .background(Color.White, RoundedCornerShape(3.dp)),
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = Seek.formatClock(position), color = Color.White)
                if (paused) Text(text = "Paused", color = Color.White)
                Text(text = Seek.formatClock(duration), color = Color.White)
            }
        }
    }
}

/** The "+10 s" / "-10 s" flash shown on the side you skipped toward. It scales in and fades out. */
@Composable
fun SkipIndicator(visible: Boolean, label: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 1.3f),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
                .padding(horizontal = 32.dp, vertical = 16.dp),
        )
    }
}
