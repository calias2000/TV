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
 * Same convention as OwnTV: CH+ and D-pad up go to the NEXT item, CH- and D-pad down go to the
 * PREVIOUS one (see Zap.step). On live, D-pad up/down zap; on movies they show the controls.
 * [controlsEnabled] is false while an error shows, so OK and left/right reach the Retry button.
 */
fun Modifier.playerKeys(
    isLive: Boolean,
    controlsEnabled: Boolean,
    onZap: (delta: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSkip: (forward: Boolean, repeatCount: Int) -> Unit,
    onShowControls: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val controls = !isLive && controlsEnabled
    when (event.key) {
        Key.ChannelUp -> { onZap(+1); true }
        Key.ChannelDown -> { onZap(-1); true }
        Key.DirectionUp -> { if (isLive) onZap(+1) else onShowControls(); true }
        Key.DirectionDown -> { if (isLive) onZap(-1) else onShowControls(); true }
        Key.DirectionCenter, Key.Enter -> if (controls) { onTogglePlay(); true } else false
        Key.DirectionLeft -> if (controls) { onSkip(false, event.nativeKeyEvent.repeatCount); true } else false
        Key.DirectionRight -> if (controls) { onSkip(true, event.nativeKeyEvent.repeatCount); true } else false
        Key.MediaPlayPause -> { onTogglePlay(); true }
        else -> false
    }
}
