package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

// Timing of the movie/episode controls and the skip flash. Our own starting values.
private val CONTROLS_HIDE_DELAY = 4.seconds
private val SKIP_FLASH = 700.milliseconds

/**
 * The "+10 s" flash: how far the skips in a row have moved, forward (positive) or back (negative).
 * [id] makes two identical skips in a row count as two events.
 */
data class SkipFlash(val totalMs: Long, val id: Int) {
    val forward get() = totalMs >= 0
    val label get() = (if (forward) "+" else "-") + abs(totalMs) / 1000 + " s"
}

/**
 * When the movie/episode controls (the bar and the play/pause icon, always together) show: on start,
 * UP, OK and seek. They hide after [CONTROLS_HIDE_DELAY], or at once on BACK ([hide]). While paused
 * they stay until the user plays again. The skip flash shows for [SKIP_FLASH]. A plain state holder.
 */
@Stable
class ControlsState {
    var shown by mutableStateOf(true)
        private set
    var showToken by mutableIntStateOf(0)
        private set
    var skip by mutableStateOf(SkipFlash(totalMs = 0, id = 0))
        private set
    var skipVisible by mutableStateOf(false)
        private set

    fun show() {
        shown = true
        showToken++ // restarts the hide countdown
    }

    fun hide() {
        shown = false
    }

    /** Skips in a row in the same direction add up ("+10 s", "+20 s", ...). [deltaMs] is negative to go back. */
    fun flashSkip(deltaMs: Long) {
        val sameWay = skipVisible && (skip.totalMs >= 0) == (deltaMs >= 0)
        skip = SkipFlash(if (sameWay) skip.totalMs + deltaMs else deltaMs, skip.id + 1)
        skipVisible = true
        show()
    }

    internal suspend fun runSkipTimer() {
        if (!skipVisible) return
        delay(SKIP_FLASH)
        skipVisible = false
    }
}

/** [paused]: while true the controls do not hide. Playing again starts the countdown. */
@Composable
fun rememberControlsState(paused: Boolean): ControlsState {
    val state = remember { ControlsState() }
    LaunchedEffect(state.showToken, paused) {
        if (!paused) {
            delay(CONTROLS_HIDE_DELAY)
            state.hide()
        }
    }
    LaunchedEffect(state.skip) { state.runSkipTimer() }
    return state
}
