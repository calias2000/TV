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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.tv.material3.Text
import com.calias.tv.app.rememberNowMs
import com.calias.tv.app.ui.Edge
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import com.calias.tv.core.seek.Seek
import com.calias.tv.core.series.Episodes

/** How long before the end the "Next episode" card appears. Our own starting value. */
private const val NEXT_OFFER_MS = 30_000L

/**
 * Plays a channel, movie, or episode full screen. This file only wires things together; each part
 * lives in its own file:
 * - [buildPlayer]: the ExoPlayer engine and its configuration.
 * - Media3's Compose UI module ([ContentFrame] for the video, state holders for playback state,
 *   error and progress): the 2026 official way, instead of wrapping the old PlayerView.
 * - [ZapState]: which channel is shown and loaded, with the zap debounce.
 * - [PanelState] and [ChannelPanel]: the glass browse panels over the video.
 * - [ControlsState]: when the header, the progress bar and the skip flash show.
 * - [playerKeys]: the remote-control keys.
 * - [VodOverlay], [LiveChannelLabel], [BufferingPill], [ErrorPanel]: what is drawn on the video.
 *
 * Live ([isLive]): every arrow key opens the panel, channel keys zap. Movies and episodes: UP shows
 * the title and progress bar, DOWN opens the panel, OK pauses, left/right seek, channel keys step to
 * the previous or next item.
 * [groups] are what the panel browses (live categories, or one flat list).
 *
 * Live opens with the panel already open over the playing channel. Moving in it only browses. OK on a
 * channel plays it and keeps the panel, OK on the one that plays closes it for a clean picture. BACK
 * closes the panel, and BACK again leaves. [startPlaying] false waits for the first OK.
 *
 * Movies and episodes: [startPosition] says where to start an item (resume), [onProgress] reports where
 * the user is. Live: [onTuned] reports each channel that starts. Each defaults to doing nothing.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    groups: List<Category>,
    initialGroup: Int,
    initialIndex: Int,
    isLive: Boolean,
    startPlaying: Boolean,
    guide: Guide,
    onExit: () -> Unit,
    startPosition: suspend (Channel) -> Long = { 0L },
    onProgress: (Channel, positionMs: Long, durationMs: Long) -> Unit = { _, _, _ -> },
    onTuned: (Channel) -> Unit = {},
) {
    val context = LocalContext.current
    val player = remember { buildPlayer(context) }
    val zap = rememberZapState(groups, initialGroup, initialIndex)
    val controls = rememberControlsState()
    val panel = remember { PanelState(startOpen = isLive, group = initialGroup) }
    val nowMs = rememberNowMs()
    val channel = zap.channels[zap.loaded]
    var started by remember { mutableStateOf(startPlaying) }

    // State holders: they listen to the player and expose Compose state.
    val flags = rememberPlaybackFlags(player)
    val error = rememberErrorState(player).error
    val progress = rememberProgressStateWithTickInterval(player, tickIntervalMs = 1_000)
    val paused = flags.paused
    // Next episode: offered during the last seconds, and started by itself when the episode ends.
    val nextIndex = remember(isLive, zap.group, zap.loaded) {
        if (isLive) null else Episodes.nextIndex(zap.channels, zap.loaded)
    }
    val nearEnd by remember {
        // derivedStateOf: the screen recomposes when this flips, not on every progress tick.
        derivedStateOf { progress.durationMs > 0 && progress.durationMs - progress.currentPositionMs <= NEXT_OFFER_MS }
    }
    // Bumped by the Retry button to start the same stream again.
    var retryToken by remember { mutableIntStateOf(0) }

    // Remember where a movie or episode is, so it can resume later. Only once the length is known,
    // so leaving while the stream is still loading cannot erase an earlier record.
    // rememberUpdatedState: the lifecycle and dispose callbacks must see the current channel.
    val saveProgress by rememberUpdatedState {
        if (!isLive && player.duration > 0) onProgress(channel, player.currentPosition, player.duration)
    }

    fun playNext() {
        nextIndex?.let {
            saveProgress()
            zap.select(zap.group, it)
        }
    }

    // Start the stream when the loaded channel changes, or when Retry is pressed.
    LaunchedEffect(channel.streamUrl, retryToken, started) {
        if (!started) return@LaunchedEffect
        if (isLive) onTuned(channel)
        val start = if (isLive) 0L else startPosition(channel)
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl), start)
        player.prepare()
        player.playWhenReady = true
        controls.showOverlay()
    }

    LaunchedEffect(flags.ended) { if (flags.ended) playNext() }

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
    // Back in front, a live channel tunes in again (its old stream is stale); movies wait paused for OK.
    var wasStopped by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        saveProgress()
        player.pause()
        wasStopped = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (wasStopped && isLive && started) retryToken++
        wasStopped = false
    }
    // Declared after the exit handler, so it wins while the panel is open: BACK closes it first.
    BackHandler(onBack = onExit)
    BackHandler(enabled = panel.visible, onBack = panel::close)

    // Key presses go to whichever composable has focus. While an error shows, focus is on the Retry
    // button (ErrorPanel asks for it); while the panel is open, on its rows (it asks for it);
    // otherwise it stays on the screen itself.
    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(error == null, panel.visible) {
        if (error == null && !panel.visible) screenFocus.requestFocus()
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
                listOpen = panel.visible,
                nextEpisodeOffered = nextIndex != null && nearEnd,
                onZap = {
                    zap.zap(it)
                    controls.showOverlay() // show the header for the channel zapped to
                },
                onTogglePlay = {
                    if (player.playWhenReady) player.pause() else player.play()
                    controls.showOverlay()
                },
                onSkip = { forward, repeatCount ->
                    val step = Seek.stepMs(repeatCount)
                    player.seekTo(Seek.target(player.currentPosition, if (forward) step else -step, player.duration))
                    controls.flashSkip((if (forward) "+" else "-") + (step / 1000) + " s", forward)
                },
                onShowInfo = controls::showOverlay,
                onOpenPanel = { panel.open(zap.group) },
                onNextEpisode = ::playNext,
            ),
    ) {
        ContentFrame(player = player, modifier = Modifier.fillMaxSize())

        if (isLive) {
            val shown = zap.channels[zap.pending]
            val nowNext = guide.nowNext(shown.tvgId, nowMs)
            // Shown on start and on zap, then fades out. The panel covers the same corner.
            LiveChannelLabel(
                visible = controls.overlayShown && !panel.visible,
                name = shown.name,
                now = nowNext?.now?.title,
                next = nowNext?.next?.title,
                modifier = Modifier.align(Alignment.TopStart),
            )
        } else {
            VodOverlay(
                visible = controls.overlayShown || paused,
                title = zap.channels[zap.pending].name,
                positionMs = { progress.currentPositionMs },
                durationMs = { progress.durationMs },
                paused = paused,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            NextEpisodeCard(
                visible = nextIndex != null && nearEnd,
                title = nextIndex?.let { zap.channels[it].name }.orEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Edge.X, bottom = 220.dp),
            )
            SkipIndicator(
                visible = controls.skipVisible,
                label = controls.skip.label,
                modifier = Modifier
                    .align(if (controls.skip.forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(96.dp),
            )
        }

        ChannelPanel(
            state = panel,
            groups = groups,
            playingGroup = zap.group,
            playingIndex = zap.pending,
            started = started,
            guide = guide,
            nowMs = nowMs,
            showGuide = isLive,
            onPick = { group, index ->
                if (isLive && started && group == zap.group && index == zap.loaded) {
                    panel.close() // OK on what plays: a clean picture
                } else {
                    saveProgress()
                    zap.select(group, index)
                    started = true
                    if (!isLive) panel.close() // movies and episodes: watch at once. Live: keep browsing.
                }
            },
        )

        // Nothing remembered to play yet: say what to do (the panel is open over this).
        if (!started) {
            Text(
                text = "Pick a channel",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // The pill and the error panel sit mid-screen, between the panels: they wait for the panel to close.
        if (flags.buffering && error == null && !panel.visible) BufferingPill(Modifier.align(Alignment.Center))
        if (error != null && !panel.visible) {
            ErrorPanel(
                code = error.errorCodeName,
                onRetry = { retryToken++ },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
