package com.calias.tv.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * When a live stream drops (the provider closes it, or the network or the decoder fails), tune in again
 * by itself instead of leaving a dead screen: after 1 s, then 2 s, then 4 s. If every one of those drops
 * again, [exhausted] turns on and the screen shows the failure with its Retry button.
 * The wait gives the provider time to free the connection (many accounts allow one stream at a time) and
 * keeps a channel that closes at once from being hammered. A plain state holder, one per channel.
 */
@Stable
class LiveRetry(private val waits: List<Duration> = listOf(1.seconds, 2.seconds, 4.seconds)) {

    private var used = 0

    var exhausted by mutableStateOf(false)
        private set

    /** The stream dropped: wait, then call [retune]. When the waits are used up it only sets [exhausted]. */
    suspend fun onDrop(retune: () -> Unit) {
        if (used >= waits.size) {
            exhausted = true
            return
        }
        delay(waits[used])
        used++ // counted once the wait is over, so a cancelled wait (a zap) costs nothing
        retune()
    }

    /** The stream is steady again, or the user pressed Retry: start again from the first wait. */
    fun reset() {
        used = 0
        exhausted = false
    }
}
