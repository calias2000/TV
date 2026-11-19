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
import androidx.compose.ui.platform.LocalView
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
import com.calias.tv.app.TrackPrefs
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import com.calias.tv.core.seek.Seek
import com.calias.tv.core.series.Episodes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** How long before the end the "Next episode" card appears. */
private const val NEXT_OFFER_MS = 60_000L

/** A live stream that played this long without dropping counts as steady: the retries start over. */
private val STEADY_AFTER = 10.seconds

/**
 * Plays a channel, movie, or episode full screen. This file only wires things together; each part
 * lives in its own file:
 * - [buildPlayer]: the ExoPlayer and its configuration.
 * - Media3's Compose UI module: [ContentFrame] for the video, state holders for error and progress.
 * - [ZapState]: which channel is shown and loaded, with the zap debounce.
 * - [PanelState] and [ChannelPanel]: the glass browse panels over the video.
 * - [ControlsState]: when the movie bar and the skip flash show, and which of the bar's Audio and
 *   Subtitles buttons is highlighted.
 * - [TracksState] and [TrackMenuCard]: the audio and subtitle tracks of the file, and the card to pick one.
 * - [playerKeys]: the remote-control keys.
 * - [VodOverlay], [PlayPauseIcon], [NextEpisodeCard], [SkipIndicator], [LoadingSpinner], [ErrorPanel]:
 *   what is drawn on the video.
 *
 * Live ([isLive]) opens with the panel already open over the playing channel; every arrow key opens it
 * and channel keys zap. Moving in the panel only browses: OK on a channel plays it and keeps the panel,
 * OK on the one that plays closes it for a clean picture. [startPlaying] false waits for the first OK.
 * Movies and episodes: UP shows the bar (it stays while paused), DOWN opens the panel (with the bar
 * showing, DOWN highlights its Audio and Subtitles buttons instead), OK pauses, left/right seek, channel
 * keys step to the previous or next item.
 *
 * BACK steps back one layer: the card, the panel, the next-episode offer, the bar, and then it leaves.
 * [groups] are what the panel browses (live categories, or one flat list).
 *
 * Movies and episodes: [startPosition] says where to start an item (resume), [onProgress] reports where
 * the user is, [subtitleSize] is the size of the subtitles (changed in the subtitles card: [onSubtitleSize]),
 * [trackPrefs] is the audio and subtitle language chosen last, applied before each item starts, and
 * [onTrackChoice] hears each new choice. Live: [onTuned] reports each channel that
 * starts. Each defaults to doing nothing (or the default size).
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
    subtitleSize: SubtitleSize = SubtitleSize.DEFAULT,
    onSubtitleSize: (SubtitleSize) -> Unit = {},
    trackPrefs: suspend () -> TrackPrefs = { TrackPrefs() },
    onTrackChoice: (TrackMenu, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val player = remember { buildPlayer(context) }
    val zap = rememberZapState(groups, initialGroup, initialIndex)
    val panel = remember { PanelState(startOpen = isLive, group = initialGroup) }
    val nowMs = rememberNowMs()
    val channel = zap.channels[zap.loaded]
    var started by remember { mutableStateOf(startPlaying) }

    // State holders: they listen to the player and expose Compose state.
    val flags = rememberPlaybackFlags(player)
    val error = rememberErrorState(player).error
    val progress = rememberProgressStateWithTickInterval(player, tickIntervalMs = 1_000)
    val paused = flags.paused
    val controls = rememberControlsState(paused)
    val tracks = rememberTracksState(player, onTrackChoice)
    // Next episode: offered during the last seconds, and started by itself when the episode ends.
    val nextIndex = remember(isLive, zap.group, zap.loaded) {
        if (isLive) null else Episodes.nextIndex(zap.channels, zap.loaded)
    }
    val nearEnd by remember {
        // derivedStateOf: the screen recomposes when this flips, not on every progress tick.
        derivedStateOf { progress.durationMs > 0 && progress.durationMs - progress.currentPositionMs <= NEXT_OFFER_MS }
    }
    // BACK on the card keeps the user in this episode: no card, no automatic start.
    var nextCancelled by remember(zap.group, zap.loaded) { mutableStateOf(false) }
    val nextOffered = nextIndex != null && nearEnd && !nextCancelled
    // Bumped to start the same stream again: by the Retry button, and by the live retries below.
    var retryToken by remember { mutableIntStateOf(0) }

    // Live: a dropped stream (closed by the provider, or a failure) is tuned in again by itself, a few
    // times, before the failure is shown. Movies and episodes show a failure at once.
    val retry = remember(channel.streamUrl) { LiveRetry() }
    val failure = remember(error, flags.ended, isLive) { error?.let(Failure::of) ?: Failure.ENDED.takeIf { isLive && flags.ended } }
    val reconnecting = isLive && failure != null && !retry.exhausted
    val shownFailure = failure.takeUnless { reconnecting }
    val showFailure = shownFailure != null

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
        if (!isLive) player.trackSelectionParameters = player.trackSelectionParameters.withPrefs(trackPrefs())
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl), start)
        player.prepare()
        player.playWhenReady = true
        controls.show()
    }

    LaunchedEffect(flags.ended) { if (flags.ended && !nextCancelled) playNext() }

    // Load the zapped-to channel once the user stops pressing keys.
    LaunchedEffect(zap.pending) { zap.settle(beforeLoad = saveProgress) }

    // Leaving the screen: save, then free the player.
    DisposableEffect(player) {
        onDispose {
            saveProgress()
            player.release()
        }
    }
    // The TV screen saver must not start while something plays (Media3 recommends keepScreenOn for video).
    val view = LocalView.current
    DisposableEffect(view, paused) {
        view.keepScreenOn = !paused
        onDispose { view.keepScreenOn = false }
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
    // Not while the app is in the background: tuning in again would start the sound there.
    LaunchedEffect(retry, failure) {
        if (isLive && started && failure != null && !wasStopped) retry.onDrop { if (!wasStopped) retryToken++ }
    }
    // Steady for a while: the next drop starts again from the shortest wait.
    val steady = started && !flags.buffering && failure == null
    LaunchedEffect(retry, steady) {
        if (steady) {
            delay(STEADY_AFTER)
            retry.reset()
        }
    }
    // The movie bar and its icon, shown only when nothing else is in the way. An open Audio/Subtitles card
    // takes their place, and they come back when it closes.
    val controlsVisible = !isLive && controls.shown && !panel.visible && !controls.menuOpen && !showFailure
    // Each handler is declared after the ones it must win over: BACK closes the card first, then the
    // panel, then cancels the next-episode offer, then hides the bar, and only then leaves.
    BackHandler(onBack = onExit)
    BackHandler(enabled = controlsVisible, onBack = controls::hide)
    BackHandler(enabled = nextOffered, onBack = { nextCancelled = true })
    BackHandler(enabled = panel.visible, onBack = panel::close)
    BackHandler(enabled = controls.menuOpen, onBack = controls::closeMenu)

    // A failed stream closes the browse panel and the movie bar with its card, so the error and its Retry
    // button are in view and nothing else holds focus.
    LaunchedEffect(showFailure) {
        if (showFailure) {
            panel.close()
            controls.hide()
        }
    }

    // Key presses go to whichever composable has focus. While an error shows, focus is on the Retry
    // button (ErrorPanel asks for it); while the panel is open, on its rows (it asks for it);
    // otherwise it stays on the screen itself.
    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(showFailure, panel.visible, controls.menuOpen) {
        if (!showFailure && !panel.visible && !controls.menuOpen) screenFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocus)
            .focusable()
            .playerKeys(
                isLive = isLive,
                controlsEnabled = !showFailure,
                listOpen = panel.visible || controls.menuOpen,
                barOpen = controlsVisible,
                nextEpisodeOffered = nextOffered,
                onZap = { zap.zap(it) },
                onTogglePlay = {
                    if (player.playWhenReady) player.pause() else player.play()
                    controls.show()
                },
                onSkip = { forward, repeatCount ->
                    val step = Seek.stepMs(repeatCount)
                    val delta = if (forward) step else -step
                    player.seekTo(Seek.target(player.currentPosition, delta, player.duration))
                    controls.flashSkip(delta)
                },
                onShowControls = controls::show,
                onOpenPanel = { panel.open(zap.group) },
                onNextEpisode = ::playNext,
                onBarKey = controls::onBarKey,
                onBarKeyUp = controls::onBarKeyUp,
            ),
    ) {
        ContentFrame(player = player, modifier = Modifier.fillMaxSize())

        if (!isLive) {
            VodOverlay(
                visible = controlsVisible,
                title = zap.channels[zap.pending].name,
                positionMs = { progress.currentPositionMs },
                durationMs = { progress.durationMs },
                highlighted = controls.button,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            SubtitleText(
                text = tracks.subtitle,
                size = subtitleSize,
                raised = controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            // The spinner takes the middle while the player waits for data.
            PlayPauseIcon(
                visible = controlsVisible && !flags.buffering,
                paused = paused,
                modifier = Modifier.align(Alignment.Center),
            )
            NextEpisodeCard(
                visible = nextOffered,
                title = nextIndex?.let { zap.channels[it].name }.orEmpty(),
                remainingFraction = {
                    ((progress.durationMs - progress.currentPositionMs).toFloat() / NEXT_OFFER_MS).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Edge.Margin, bottom = 220.dp),
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

        if (!isLive) {
            TrackMenuCard(
                visible = controls.menuOpen,
                menu = controls.button ?: TrackMenu.AUDIO,
                tracks = tracks,
                subtitleSize = subtitleSize,
                onSubtitleSize = onSubtitleSize,
                onPicked = controls::closeMenu,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        // Nothing remembered to play yet: say what to do (the panel is open over this).
        if (!started) {
            Text(
                text = "Pick a channel",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Mid-screen, between the two cards, so it shows with the panel open too. The error panel needs focus
        // for its Retry button, so it waits for the panel to close (an error closes the panel, see above).
        if (!showFailure && (flags.buffering || reconnecting)) LoadingSpinner(Modifier.align(Alignment.Center))
        if (shownFailure != null && !panel.visible) {
            ErrorPanel(
                failure = shownFailure,
                onRetry = {
                    retry.reset()
                    retryToken++
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
