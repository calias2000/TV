package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.R
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassButton
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.ProgressBar
import com.calias.tv.app.ui.Spinner

// Small overlays drawn on top of the video. Fixed white-on-dark colors: they sit on video in
// either theme.

/** The round dark disc behind the icons in the middle of the screen, so they read on any picture. */
@Composable
private fun Disc(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.size(88.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Shown while the player waits for data: at start, after a seek, on a slow network. */
@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Disc(modifier) { Spinner(diameter = 44.dp, color = Color.White) }
}

/**
 * The big icon in the middle of the screen, shown with the movie bar: what OK does now (play while
 * paused, pause while playing). [visible] fades it in and out.
 */
@Composable
fun PlayPauseIcon(visible: Boolean, paused: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Disc(Modifier) {
            Icon(
                painter = painterResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = if (paused) "Play" else "Pause",
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

/** What went wrong, in words for the family. The Media3 code name stays visible below it for us. */
private fun message(error: PlaybackException) = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Can't reach the stream"
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Stream not available"
    else -> "Playback failed"
}

/**
 * Playback failed: a message and a Retry button that takes focus, so OK works. The screen leaves
 * this panel out while the browse panel is open (an error closes that panel by itself), so it takes focus again each time it comes back.
 */
@Composable
fun ErrorPanel(error: PlaybackException, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.requestFocus() }
    GlassPanel(modifier) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message(error), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(text = error.errorCodeName, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            GlassButton(text = "Retry", onClick = onRetry, modifier = Modifier.focusRequester(retryFocus), colors = Glass.overVideo)
            Text(text = "Back to leave", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

/**
 * "Next episode" offer, bottom-right, shown near the end of an episode (like Netflix). OK plays it
 * at once, BACK cancels it, and if the episode just ends the next one starts by itself. The bar
 * counts down: [remainingFraction] goes from 1 to 0 over the offer.
 */
@Composable
fun NextEpisodeCard(visible: Boolean, title: String, remainingFraction: () -> Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        GlassPanel(Modifier.width(320.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "Next episode  ·  OK to play", color = Color.White)
                Text(text = title, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProgressBar(fraction = remainingFraction(), modifier = Modifier.padding(top = 8.dp), height = 6.dp, onVideo = true)
            }
        }
    }
}
