package com.calias.tv.app.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Remote-control keys of the player screen.
 *
 * Same convention as OwnTV: CH+ goes to the NEXT item, CH- to the PREVIOUS one (see Zap.step).
 * - Live: UP, DOWN, LEFT and RIGHT all open the browse panel. CH+/CH- zap (the channel header
 *   shows on a zap, since the panel is closed then).
 * - Movies and episodes: UP shows the title and the progress bar, DOWN opens the panel, LEFT/RIGHT
 *   seek. OK pauses, or plays the next episode while the "Next episode" card is offered.
 *   CH+/CH- step to the previous/next item.
 * - Panel open ([listOpen]): it owns the D-pad and OK (they reach the focused row); channel keys still zap.
 * - Error showing ([controlsEnabled] false): LEFT/RIGHT/OK reach the Retry button, DOWN still
 *   opens the panel, so a dead channel can be left without channel keys.
 */
fun Modifier.playerKeys(
    isLive: Boolean,
    controlsEnabled: Boolean,
    listOpen: Boolean,
    nextEpisodeOffered: Boolean,
    onZap: (delta: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSkip: (forward: Boolean, repeatCount: Int) -> Unit,
    onShowInfo: () -> Unit,
    onOpenPanel: () -> Unit,
    onNextEpisode: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val controls = !isLive && controlsEnabled
    val panelKey = isLive && controlsEnabled
    when (event.key) {
        Key.ChannelUp -> { onZap(+1); true }
        Key.ChannelDown -> { onZap(-1); true }
        Key.MediaPlayPause -> { onTogglePlay(); true }
        else -> when {
            listOpen -> false
            event.key == Key.DirectionUp -> { if (isLive) onOpenPanel() else onShowInfo(); true }
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
