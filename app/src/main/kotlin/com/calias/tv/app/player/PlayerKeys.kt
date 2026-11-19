package com.calias.tv.app.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Remote-control keys of the player screen. CH+ goes to the next item, CH- to the previous one.
 * - Live: UP, DOWN, LEFT and RIGHT all open the browse panel. CH+/CH- zap.
 * - Movies and episodes: UP shows the bar, DOWN opens the panel (while the bar shows, the bar takes DOWN
 *   and the keys after it: [onBarKey]), LEFT/RIGHT seek. OK pauses, or plays the next episode while the
 *   "Next episode" card is offered. CH+/CH- step to the previous/next item.
 * - Panel open ([listOpen]): it owns the D-pad and OK (they reach the focused row); channel keys still zap.
 * - Buttons act when OK is let go, so the bar's OK is used on the key release ([onBarKeyUp]).
 * - Error showing ([controlsEnabled] false): LEFT/RIGHT/OK reach the Retry button, DOWN still
 *   opens the panel, so a dead channel can be left without channel keys.
 */
fun Modifier.playerKeys(
    isLive: Boolean,
    controlsEnabled: Boolean,
    listOpen: Boolean,
    barOpen: Boolean,
    nextEpisodeOffered: Boolean,
    onZap: (delta: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSkip: (forward: Boolean, repeatCount: Int) -> Unit,
    onShowControls: () -> Unit,
    onOpenPanel: () -> Unit,
    onNextEpisode: () -> Unit,
    onBarKey: (Key) -> Boolean,
    onBarKeyUp: (Key) -> Boolean,
): Modifier = onPreviewKeyEvent { event ->
    val controls = !isLive && controlsEnabled
    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent controls && barOpen && onBarKeyUp(event.key)
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val panelKey = isLive && controlsEnabled
    when (event.key) {
        Key.ChannelUp -> { onZap(+1); true }
        Key.ChannelDown -> { onZap(-1); true }
        Key.MediaPlayPause -> { onTogglePlay(); true }
        else -> when {
            listOpen -> false
            controls && barOpen && onBarKey(event.key) -> true
            event.key == Key.DirectionUp -> { if (isLive) onOpenPanel() else onShowControls(); true }
            event.key == Key.DirectionDown -> { onOpenPanel(); true }
            event.key == Key.DirectionCenter || event.key == Key.Enter ->
                if (controls) { if (nextEpisodeOffered) onNextEpisode() else onTogglePlay(); true } else false
            event.key == Key.DirectionLeft || event.key == Key.DirectionRight -> when {
                panelKey -> { onOpenPanel(); true }
                controls -> { onSkip(event.key == Key.DirectionRight, event.nativeKeyEvent.repeatCount); true }
                else -> false
            }
            else -> false
        }
    }
}
