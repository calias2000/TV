package com.calias.tv.app.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.browse.ProgramGuide
import com.calias.tv.app.formatTime
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** How long the cursor must rest on a channel before the guide card shows that channel. Our own value. */
private val GUIDE_DELAY = 150.milliseconds

/**
 * The browse panels over the full-screen video: a floating glass card on the left to pick a channel,
 * movie or episode without leaving the one that plays (it keeps playing), and, for live
 * ([showGuide]), a second card on the right with the guide of the channel under the cursor.
 *
 * Level CHANNELS lists the browsed group, level GROUPS lists all groups (live only).
 * UP/DOWN move, OK picks. LEFT goes from the channels to the groups. RIGHT goes from the groups back
 * to the channels, and from the channels into the guide card, where UP/DOWN scroll the day and LEFT
 * returns to the channels. BACK (handled by the caller) closes the panel.
 * Moving only browses, nothing starts: the guide follows the cursor, the picture stays on [playingIndex].
 * Fixed light-on-dark colors: it sits on video, in either theme.
 */
@Composable
fun ChannelPanel(
    state: PanelState,
    groups: List<Category>,
    playingGroup: Int,
    playingIndex: Int,
    started: Boolean,
    guide: Guide,
    nowMs: Long,
    showGuide: Boolean,
    onPick: (group: Int, index: Int) -> Unit,
) {
    AnimatedVisibility(
        visible = state.visible,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally { -it / 8 } + fadeIn(),
        exit = slideOutHorizontally { -it / 8 } + fadeOut(),
    ) {
        val hasGroups = groups.size > 1
        var underCursor by remember { mutableStateOf<Channel?>(null) }
        var guideChannel by remember { mutableStateOf<Channel?>(null) }
        LaunchedEffect(Unit) {
            snapshotFlow { underCursor }.collectLatest {
                delay(GUIDE_DELAY)
                guideChannel = it
            }
        }
        var guideFocused by remember { mutableStateOf(false) }
        val guideEntry = remember { FocusRequester() }
        val listFocus = remember(state.level, state.group) { FocusRequester() }
        BackHandler(enabled = guideFocused) { listFocus.requestFocus() }
        val shown = guideChannel ?: groups[playingGroup].channels[playingIndex]
        val schedule = if (showGuide) guide.schedule(shown.tvgId, nowMs) else emptyList()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Edge.Margin)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            when {
                                guideFocused -> listFocus.requestFocus()
                                hasGroups && state.level == PanelLevel.CHANNELS -> state.showGroups()
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            when {
                                guideFocused -> Unit
                                state.level == PanelLevel.GROUPS -> state.showChannels(state.group)
                                underCursor != null && underCursor?.id != shown.id -> guideChannel = underCursor
                                schedule.isNotEmpty() -> guideEntry.requestFocus()
                            }
                            true
                        }
                        else -> false
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassPanel(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 16.dp)) {
                    val title = if (state.level == PanelLevel.GROUPS) "Groups" else groups[state.group].name
                    PanelTitle(title)
                    // New list (and new initial focus) whenever the level or browsed group changes.
                    composeKey(state.level, state.group) {
                        if (state.level == PanelLevel.GROUPS) {
                            BrowseList(count = groups.size, focusIndex = state.group) { index, focus ->
                                ChannelRow(
                                    title = groups[index].name,
                                    trailing = groups[index].channels.size.toString(),
                                    playing = index == playingGroup,
                                    onVideo = true,
                                    modifier = focus,
                                    onClick = { state.showChannels(index) },
                                )
                            }
                        } else {
                            val channels = groups[state.group].channels
                            val here = started && state.group == playingGroup
                            BrowseList(
                                count = channels.size,
                                focusIndex = if (state.group == playingGroup) playingIndex else 0,
                                focusRequester = listFocus,
                            ) { index, focus ->
                                val channel = channels[index]
                                val now = guide.nowNext(channel.tvgId, nowMs)?.now
                                ChannelRow(
                                    title = channel.name,
                                    channel = channel,
                                    subtitle = now?.let { "${it.title} · ${formatTime(it.stopMs)}" },
                                    progress = now?.progress(nowMs),
                                    playing = here && index == playingIndex,
                                    onVideo = true,
                                    modifier = focus.onFocusChanged { if (it.hasFocus) underCursor = channel },
                                    onClick = { onPick(state.group, index) },
                                )
                            }
                        }
                    }
                }
            }
            if (showGuide) {
                GlassPanel(modifier = Modifier.width(300.dp).fillMaxHeight().onFocusChanged { guideFocused = it.hasFocus }) {
                    composeKey(shown.id) { ProgramGuide(shown.name, schedule, nowMs, guideEntry) } // each channel starts at the top
                }
            }
        }
    }
}

/** The title of a glass list card over the video ("Groups", "Audio", ...). */
@Composable
internal fun PanelTitle(text: String) {
    Text(
        text = text,
        color = Glass.overVideo.text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
    )
}
