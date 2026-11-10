package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.ProgressBar
import com.calias.tv.core.seek.Seek

/**
 * The bar for movies and episodes, a glass panel floating at the bottom: title, a progress bar that
 * glides to each new position, and elapsed / total time. White on dark glass, because it sits on
 * top of video in either theme.
 */
@Composable
fun VodOverlay(
    visible: Boolean,
    title: String,
    positionMs: () -> Long,
    durationMs: () -> Long,
    highlighted: TrackMenu?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        GlassPanel(Modifier.fillMaxWidth().padding(Edge.Margin)) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TrackMenu.entries.forEach { BarButton(it.label, highlighted = it == highlighted) }
                }
                // Read here, not in the caller, so each progress tick recomposes only this bar.
                val position = positionMs()
                val duration = durationMs()
                ProgressBar(
                    fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                    height = 6.dp,
                    onVideo = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = Seek.formatClock(position), color = Color.White)
                    Text(text = Seek.formatClock(duration), color = Color.White)
                }
            }
        }
    }
}

/**
 * An Audio or Subtitles button of the bar. It cannot take focus: [ControlsState] says which one is
 * [highlighted], and it looks like a focused glass button then.
 */
@Composable
private fun BarButton(text: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(shape)
            .background(if (highlighted) Glass.overVideo.fillFocused else Color.White.copy(alpha = 0.08f))
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) Glass.overVideo.edgeFocused else Color.White.copy(alpha = 0.14f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp))
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
        GlassPanel {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            )
        }
    }
}
