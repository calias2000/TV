package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Which item is shown ([pending], changes on every key press) and which is loaded ([loaded],
 * follows [pending] after [ZAP_DEBOUNCE]). A plain state holder: UI logic only, no Android types.
 */
@Stable
class ZapState(private val size: Int, initial: Int) {
    var loaded by mutableIntStateOf(initial)
        private set
    var pending by mutableIntStateOf(initial)
        private set

    fun zap(delta: Int) {
        pending = Zap.step(pending, delta, size)
    }

    /** Waits for the keys to stop, then loads [pending]. [beforeLoad] runs while the old item is still loaded. */
    suspend fun settle(beforeLoad: () -> Unit) {
        if (pending == loaded) return
        delay(ZAP_DEBOUNCE)
        beforeLoad()
        loaded = pending
    }
}

@Composable
fun rememberZapState(size: Int, initial: Int): ZapState = remember { ZapState(size, initial) }
