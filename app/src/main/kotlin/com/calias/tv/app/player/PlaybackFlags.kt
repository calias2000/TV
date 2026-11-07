package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

/**
 * Playback facts the screen needs, as Compose state: is the player waiting for data ([buffering]),
 * did the user pause ([paused]), did the stream play to its end ([ended]). Media3 1.11.1's Compose module has no holder
 * for these (a general PlaybackState arrives in 1.12), so this uses the stable Player.Listener.
 * Replace with Media3's holder when we move to 1.12.
 */
@Stable
class PlaybackFlags {
    var buffering by mutableStateOf(false)
        internal set
    var paused by mutableStateOf(false)
        internal set
    var ended by mutableStateOf(false)
        internal set
}

@Composable
fun rememberPlaybackFlags(player: Player): PlaybackFlags {
    val flags = remember { PlaybackFlags() }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                flags.buffering = playbackState == Player.STATE_BUFFERING
                flags.ended = playbackState == Player.STATE_ENDED
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                flags.paused = !playWhenReady
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return flags
}
