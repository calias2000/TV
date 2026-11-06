package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category

/**
 * Browse overlay on the left of the video: a transparent panel over the picture, so the user can pick
 * another channel, movie or episode without leaving the one that plays (which keeps playing).
 * Level CHANNELS lists the browsed group, level GROUPS lists all groups (live only).
 * UP/DOWN move, OK picks, LEFT/RIGHT change level, BACK (handled by the caller) closes.
 * Fixed white-on-dark colors: it sits on video, in either theme.
 */
@Composable
fun ChannelSidebar(
    state: SidebarState,
    groups: List<Category>,
    playingGroup: Int,
    playingIndex: Int,
    guide: Guide,
    nowMs: Long,
    onPick: (group: Int, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier,
        enter = slideInHorizontally { -it / 3 } + fadeIn(),
        exit = slideOutHorizontally { -it / 3 } + fadeOut(),
    ) {
        val hasGroups = groups.size > 1
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(600.dp)
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                .padding(start = 32.dp, top = 40.dp, end = 64.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (hasGroups && state.level == SidebarLevel.CHANNELS) state.showGroups()
                            true
                        }
                        Key.DirectionRight -> {
                            if (state.level == SidebarLevel.GROUPS) state.showChannels(state.group) else state.close()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            val title = if (state.level == SidebarLevel.GROUPS) "Groups" else groups[state.group].name
            Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)

            // New list (and new initial focus) whenever the level or browsed group changes.
            composeKey(state.level, state.group) {
                if (state.level == SidebarLevel.GROUPS) {
                    SidebarList(count = groups.size, focusIndex = state.group) { index, focus ->
                        val group = groups[index]
                        SidebarRow(
                            title = "${group.name} (${group.channels.size})",
                            subtitle = null,
                            playing = index == playingGroup,
                            modifier = focus,
                            onClick = { state.showChannels(index) },
                        )
                    }
                } else {
                    val channels = groups[state.group].channels
                    SidebarList(
                        count = channels.size,
                        focusIndex = if (state.group == playingGroup) playingIndex else 0,
                    ) { index, focus ->
                        val channel = channels[index]
                        SidebarRow(
                            title = channel.name,
                            subtitle = guide.nowNext(channel.tvgId, nowMs)?.now?.title,
                            playing = state.group == playingGroup && index == playingIndex,
                            modifier = focus,
                            onClick = { onPick(state.group, index) },
                        )
                    }
                }
            }
        }
    }
}

/** A scrolling list that starts near [focusIndex] and puts focus on that row. */
@Composable
private fun SidebarList(
    count: Int,
    focusIndex: Int,
    row: @Composable (index: Int, focus: Modifier) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (focusIndex - 3).coerceAtLeast(0))
    val focus = remember { FocusRequester() }
    // The row only exists after the first frame, so wait for it before asking for focus.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focus.requestFocus()
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(count) { index ->
            row(index, if (index == focusIndex) Modifier.focusRequester(focus) else Modifier)
        }
    }
}

/** One row. Every entry is its own row, even when names repeat. [playing] marks what is on screen. */
@Composable
private fun SidebarRow(
    title: String,
    subtitle: String?,
    playing: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f), // no zoom: keeps rows aligned
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.22f),
            focusedContentColor = Color.White,
            pressedContainerColor = Color.White.copy(alpha = 0.3f),
            pressedContentColor = Color.White,
        ),
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Fixed-width marker column, so titles and subtitles line up on every row.
            Text(text = if (playing) "▶" else "", modifier = Modifier.width(28.dp))
            Column {
                Text(text = title)
                if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
