package com.calias.tv.app.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberErrorState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.calias.tv.app.rememberNowMs
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import com.calias.tv.core.seek.Seek

/**
 * Plays a channel, movie, or episode full screen. This file only wires things together; each part
 * lives in its own file:
 * - [buildPlayer]: the ExoPlayer engine and its configuration.
 * - Media3's Compose UI module ([ContentFrame] for the video, state holders for playback state,
 *   error and progress): the 2026 official way, instead of wrapping the old PlayerView.
 * - [ZapState]: which channel is shown and loaded, with the zap debounce.
 * - [SidebarState] and [ChannelSidebar]: the transparent browse overlay.
 * - [VodControlsState]: when the progress bar and the skip flash show.
 * - [playerKeys]: the remote-control keys.
 * - [VodOverlay], [LiveChannelLabel], [BufferingPill], [ErrorPanel]: what is drawn on the video.
 *
 * Live ([isLive]): channel keys and D-pad up/down zap, LEFT opens the sidebar. Movies and episodes:
 * OK pauses, left/right seek, up shows the controls, DOWN opens the sidebar, channel keys step to the
 * previous or next item. [groups] are what the sidebar browses (live categories, or one flat list).
 *
 * [startPosition] says where to start an item (resume), [onProgress] reports where the user is.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    groups: List<Category>,
    initialGroup: Int,
    initialIndex: Int,
    isLive: Boolean,
    guide: Guide,
    startPosition: suspend (Channel) -> Long,
    onProgress: (Channel, positionMs: Long, durationMs: Long) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember { buildPlayer(context) }
    val zap = rememberZapState(groups, initialGroup, initialIndex)
    val controls = rememberVodControlsState()
    val sidebar = remember { SidebarState() }
    val nowMs = rememberNowMs()
    val channel = zap.channels[zap.loaded]

    // State holders: they listen to the player and expose Compose state.
    val flags = rememberPlaybackFlags(player)
    val error = rememberErrorState(player).error
    val progress = rememberProgressStateWithTickInterval(player, tickIntervalMs = 1_000)
    val paused = flags.paused
    // Bumped by the Retry button to start the same stream again.
    var retryToken by remember { mutableIntStateOf(0) }

    // Remember where a movie or episode is, so it can resume later. Only once the length is known,
    // so leaving while the stream is still loading cannot erase an earlier record.
    // rememberUpdatedState: the lifecycle and dispose callbacks must see the current channel.
    val saveProgress by rememberUpdatedState {
        if (!isLive && player.duration > 0) onProgress(channel, player.currentPosition, player.duration)
    }

    // Start the stream when the loaded channel changes, or when Retry is pressed.
    LaunchedEffect(channel.streamUrl, retryToken) {
        val start = if (isLive) 0L else startPosition(channel)
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl), start)
        player.prepare()
        player.playWhenReady = true
        controls.showOverlay()
    }

    // Load the zapped-to channel once the user stops pressing keys.
    LaunchedEffect(zap.pending) { zap.settle(beforeLoad = saveProgress) }

    // Leaving the screen: save, then free the player.
    DisposableEffect(player) {
        onDispose {
            saveProgress()
            player.release()
        }
    }
    // App goes to the background (also TV standby): save and pause, so audio does not keep playing.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        saveProgress()
        player.pause()
    }
    BackHandler(onBack = onExit)
    // Declared after the exit handler, so it wins while the sidebar is open: BACK closes it first.
    BackHandler(enabled = sidebar.visible, onBack = sidebar::close)

    // Key presses go to whichever composable has focus. While an error shows, focus is on the Retry
    // button (ErrorPanel asks for it); while the sidebar is open, on its rows (it asks for it);
    // otherwise it stays on the screen itself.
    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(error == null, sidebar.visible) {
        if (error == null && !sidebar.visible) screenFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocus)
            .focusable()
            .playerKeys(
                isLive = isLive,
                controlsEnabled = error == null,
                sidebarOpen = sidebar.visible,
                onZap = zap::zap,
                onTogglePlay = {
                    if (player.playWhenReady) player.pause() else player.play()
                    controls.showOverlay()
                },
                onSkip = { forward, repeatCount ->
                    val step = Seek.stepMs(repeatCount)
                    player.seekTo(Seek.target(player.currentPosition, if (forward) step else -step, player.duration))
                    controls.flashSkip((if (forward) "+" else "-") + (step / 1000) + " s", forward)
                },
                onShowControls = controls::showOverlay,
                onOpenSidebar = { sidebar.open(zap.group) },
            ),
    ) {
        ContentFrame(player = player, modifier = Modifier.fillMaxSize())

        if (isLive) {
            // The sidebar covers the same corner, so the label waits until it closes.
            if (!sidebar.visible) {
                val shown = zap.channels[zap.pending]
                val nowNext = guide.nowNext(shown.tvgId, nowMs)
                LiveChannelLabel(
                    name = shown.name,
                    now = nowNext?.now?.title,
                    next = nowNext?.next?.title,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        } else {
            VodOverlay(
                visible = controls.overlayShown || paused,
                title = zap.channels[zap.pending].name,
                positionMs = { progress.currentPositionMs },
                durationMs = { progress.durationMs },
                paused = paused,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            SkipIndicator(
                visible = controls.skipVisible,
                label = controls.skip.label,
                modifier = Modifier
                    .align(if (controls.skip.forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(96.dp),
            )
        }

        ChannelSidebar(
            state = sidebar,
            groups = groups,
            playingGroup = zap.group,
            playingIndex = zap.pending,
            guide = guide,
            nowMs = nowMs,
            onPick = { group, index ->
                saveProgress()
                zap.select(group, index)
                sidebar.close()
            },
            modifier = Modifier.align(Alignment.CenterStart),
        )

        if (flags.buffering && error == null) BufferingPill(Modifier.align(Alignment.Center))
        error?.let { ErrorPanel(it.errorCodeName, onRetry = { retryToken++ }, modifier = Modifier.align(Alignment.Center)) }
    }
}
