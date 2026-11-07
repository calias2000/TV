package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

// Timing of the info overlays (live header, movie progress bar) and the skip flash. Our own starting values.
private val OVERLAY_HIDE_DELAY = 4.seconds
private val SKIP_FLASH = 700.milliseconds

/** The "+10 s" flash. [id] makes two identical skips in a row count as two events. */
data class SkipFlash(val label: String, val forward: Boolean, val id: Int)

/**
 * When the info overlays show: the live header and the movie/episode progress bar appear on start,
 * zap, UP, pause and seek, and hide after [OVERLAY_HIDE_DELAY]; the skip flash shows for [SKIP_FLASH].
 * A plain state holder, timers included.
 */
@Stable
class ControlsState {
    var overlayToken by mutableIntStateOf(0)
        private set
    var overlayShown by mutableStateOf(true)
        private set
    var skip by mutableStateOf(SkipFlash("", forward = true, id = 0))
        private set
    var skipVisible by mutableStateOf(false)
        private set

    fun showOverlay() {
        overlayToken++
    }

    fun flashSkip(label: String, forward: Boolean) {
        skip = SkipFlash(label, forward, skip.id + 1)
        skipVisible = true
        showOverlay()
    }

    internal suspend fun runOverlayTimer() {
        overlayShown = true
        delay(OVERLAY_HIDE_DELAY)
        overlayShown = false
    }

    internal suspend fun runSkipTimer() {
        if (!skipVisible) return
        delay(SKIP_FLASH)
        skipVisible = false
    }
}

@Composable
fun rememberControlsState(): ControlsState {
    val state = remember { ControlsState() }
    LaunchedEffect(state.overlayToken) { state.runOverlayTimer() }
    LaunchedEffect(state.skip) { state.runSkipTimer() }
    return state
}
