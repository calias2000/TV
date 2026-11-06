package com.calias.tv.app.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.calias.tv.core.Channel
import com.calias.tv.core.zap.Zap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * How long to wait after the last channel key before loading the stream. Several quick key presses
 * then cost one connection, not one per press. The channel name still updates on every press.
 * Our own starting value, not taken from a reference app. Tune on the real TV.
 */
private val ZAP_DEBOUNCE = 400.milliseconds

/**
 * Plays one channel full screen.
 *
 * Two Media3 parts are used:
 * - [ExoPlayer] = the engine. Downloads, buffers, decodes. Has no screen of its own.
 * - [PlayerView] = a ready-made video surface. Shown through AndroidView, controls hidden by us.
 * Everything around the video (channel label, error panel, remote keys, BACK) is our Compose code.
 */
@Composable
fun PlayerScreen(
    channels: List<Channel>,
    initialIndex: Int,
    onExit: () -> Unit,
) {
    // The channel that is loaded. Channel keys change it; navigation does not.
    var index by remember { mutableIntStateOf(initialIndex) }
    val channel = channels[index]
    val context = LocalContext.current
    val player = remember { buildPlayer(context) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // The channel the user has zapped to. It leads [index] by up to ZAP_DEBOUNCE.
    var pendingIndex by remember { mutableIntStateOf(initialIndex) }
    // Bumped by the Retry button to start the same stream again.
    var retryToken by remember { mutableIntStateOf(0) }

    // Commit the zap once the user stops pressing keys.
    LaunchedEffect(pendingIndex) {
        if (pendingIndex != index) {
            delay(ZAP_DEBOUNCE)
            index = pendingIndex
        }
    }

    // Start the stream when the committed channel changes, or when Retry is pressed.
    LaunchedEffect(channel.streamUrl, retryToken) {
        errorText = null
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    // Report playback errors on screen instead of failing silently.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = error.errorCodeName
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause when the app goes to the background, so audio does not keep playing.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // BACK leaves the player and returns to the channel list.
    BackHandler(onBack = onExit)

    // Key presses go to whichever view has focus. The native PlayerView must not take focus,
    // otherwise the remote keys never reach this Compose handler. While an error is shown, focus
    // moves to the Retry button so OK works; otherwise it stays on the screen itself.
    val screenFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(errorText != null) {
        if (errorText != null) retryFocus.requestFocus() else screenFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    // Same convention as OwnTV: CH+ and D-pad up go to the NEXT channel,
                    // CH- and D-pad down go to the PREVIOUS one. See Zap.step.
                    Key.ChannelUp, Key.DirectionUp -> {
                        pendingIndex = Zap.step(pendingIndex, +1, channels.size)
                        true
                    }
                    Key.ChannelDown, Key.DirectionDown -> {
                        pendingIndex = Zap.step(pendingIndex, -1, channels.size)
                        true
                    }
                    Key.MediaPlayPause -> {
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).also {
                    // Our own Compose UI replaces Media3's default control bar.
                    it.useController = false
                    it.isFocusable = false
                    it.isFocusableInTouchMode = false
                    it.player = player
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

        // Channel name, top-left. Shows the zapped-to channel immediately, before the stream loads.
        Text(
            text = channels[pendingIndex].name,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(48.dp),
        )

        errorText?.let { code ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "Playback failed", color = Color.White)
                Text(text = code, color = Color.White)
                Button(
                    onClick = {
                        errorText = null
                        retryToken++
                    },
                    modifier = Modifier.focusRequester(retryFocus),
                ) {
                    Text(text = "Retry")
                }
                Text(text = "Channel up/down or Back to leave", color = Color.White)
            }
        }
    }
}
