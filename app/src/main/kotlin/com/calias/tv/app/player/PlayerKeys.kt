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
 * PREVIOUS one (see Zap.step).
 * - Live: up/down zap, LEFT opens the browse sidebar.
 * - Movies and episodes: left/right seek, up shows the controls, DOWN opens the browse sidebar.
 * - Sidebar open: it owns the D-pad and OK (they reach the focused row); channel keys still zap.
 * [controlsEnabled] is false while an error shows, so OK and the D-pad reach the Retry button.
 */
fun Modifier.playerKeys(
    isLive: Boolean,
    controlsEnabled: Boolean,
    sidebarOpen: Boolean,
    onZap: (delta: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSkip: (forward: Boolean, repeatCount: Int) -> Unit,
    onShowControls: () -> Unit,
    onOpenSidebar: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val controls = !isLive && controlsEnabled
    when (event.key) {
        Key.ChannelUp -> { onZap(+1); true }
        Key.ChannelDown -> { onZap(-1); true }
        Key.MediaPlayPause -> { onTogglePlay(); true }
        else -> when {
            sidebarOpen -> false
            event.key == Key.DirectionUp -> { if (isLive) onZap(+1) else onShowControls(); true }
            event.key == Key.DirectionDown -> {
                if (isLive) onZap(-1) else if (controlsEnabled) onOpenSidebar()
                true
            }
            event.key == Key.DirectionCenter || event.key == Key.Enter ->
                if (controls) { onTogglePlay(); true } else false
            event.key == Key.DirectionLeft -> when {
                isLive && controlsEnabled -> { onOpenSidebar(); true }
                controls -> { onSkip(false, event.nativeKeyEvent.repeatCount); true }
                else -> false
            }
            event.key == Key.DirectionRight -> if (controls) { onSkip(true, event.nativeKeyEvent.repeatCount); true } else false
            else -> false
        }
    }
}
