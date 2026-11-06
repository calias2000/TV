package com.calias.tv.app.player

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
import androidx.tv.material3.Button
import androidx.tv.material3.Text

// Small overlays drawn on top of the video. Fixed white-on-dark colors: they sit on video in
// either theme.

/**
 * Live: the channel name, top-left, with what is on now and next when the guide has it. Shows the
 * zapped-to channel before its stream loads.
 */
@Composable
fun LiveChannelLabel(name: String, now: String?, next: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(48.dp)) {
        Text(text = name, color = Color.White)
        if (now != null) Text(text = now, color = Color.White)
        if (next != null) Text(text = "Next: $next", color = Color.White.copy(alpha = 0.7f))
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

/** Playback failed: the error code and a Retry button that takes focus, so OK works. */
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
        Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) {
            Text(text = "Retry")
        }
        Text(text = "Channel up/down or Back to leave", color = Color.White)
    }
}
