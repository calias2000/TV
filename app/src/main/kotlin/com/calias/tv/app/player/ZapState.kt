package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calias.tv.core.Channel
import com.calias.tv.core.library.Category
import com.calias.tv.core.zap.Zap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * How long to wait after the last channel key before loading the stream. Several quick key presses
 * then cost one connection, not one per press. The channel name still updates on every press.
 * Tune on the real TV.
 */
private val ZAP_DEBOUNCE = 400.milliseconds

/**
 * Which item is shown ([pending], changes on every key press) and which is loaded ([loaded],
 * follows [pending] after [ZAP_DEBOUNCE]), inside the current [group]. Zapping steps through that
 * group; [select] jumps to any group and item at once (panel pick). A plain state holder.
 */
@Stable
class ZapState(private val groups: List<Category>, initialGroup: Int, initialIndex: Int) {
    var group by mutableIntStateOf(initialGroup)
        private set
    var loaded by mutableIntStateOf(initialIndex)
        private set
    var pending by mutableIntStateOf(initialIndex)
        private set

    val channels: List<Channel> get() = groups[group].channels

    fun zap(delta: Int) {
        pending = Zap.step(pending, delta, channels.size)
    }

    /** Loads [index] of [group] immediately, no debounce. */
    fun select(group: Int, index: Int) {
        this.group = group
        loaded = index
        pending = index
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
fun rememberZapState(groups: List<Category>, initialGroup: Int, initialIndex: Int): ZapState =
    remember { ZapState(groups, initialGroup, initialIndex) }
