package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassButton

// Small overlays drawn on top of the video. Fixed white-on-dark colors: they sit on video in
// either theme.

/**
 * Live header, top-left: the channel name, with what is on now and next when the guide has it. Shows
 * the zapped-to channel before its stream loads. [visible] fades it in and out.
 */
@Composable
fun LiveChannelLabel(visible: Boolean, name: String, now: String?, next: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Column(modifier = Modifier.padding(horizontal = Edge.X, vertical = Edge.Y)) {
            Text(text = name, color = Color.White)
            if (now != null) Text(text = now, color = Color.White)
            if (next != null) Text(text = "Next: $next", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** Shown while the player waits for data: at start, after a seek, on a slow network. */
@Composable
fun BufferingPill(modifier: Modifier = Modifier) {
    Text(
        text = "Buffering…",
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
            .padding(horizontal = 32.dp, vertical = 16.dp),
    )
}

/**
 * Playback failed: the error code and a Retry button that takes focus, so OK works. The screen leaves
 * this panel out while the browse panel is open, so it takes focus again each time it comes back.
 */
@Composable
fun ErrorPanel(code: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.requestFocus() }
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Playback failed", color = Color.White)
        Text(text = code, color = Color.White)
        GlassButton(text = "Retry", onClick = onRetry, modifier = Modifier.focusRequester(retryFocus), colors = Glass.overVideo)
        Text(text = "Down for the list, Back to leave", color = Color.White)
    }
}

/**
 * "Next episode" offer, bottom-right, shown near the end of an episode (like Netflix). OK plays it
 * at once; if the episode just ends, the next one starts by itself.
 */
@Composable
fun NextEpisodeCard(visible: Boolean, title: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Next episode  ▶  press OK", color = Color.White)
            Text(text = title, color = Color.White.copy(alpha = 0.7f))
        }
    }
}
