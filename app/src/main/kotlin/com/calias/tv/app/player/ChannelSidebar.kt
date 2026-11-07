package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key as composeKey
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    BrowseList(count = groups.size, focusIndex = state.group) { index, focus ->
                        ChannelRow(
                            title = "${groups[index].name} (${groups[index].channels.size})",
                            playing = index == playingGroup,
                            onVideo = true,
                            modifier = focus,
                            onClick = { state.showChannels(index) },
                        )
                    }
                } else {
                    val channels = groups[state.group].channels
                    val here = state.group == playingGroup
                    BrowseList(count = channels.size, focusIndex = if (here) playingIndex else 0) { index, focus ->
                        val channel = channels[index]
                        val now = guide.nowNext(channel.tvgId, nowMs)?.now
                        ChannelRow(
                            title = channel.name,
                            channel = channel,
                            subtitle = now?.title,
                            progress = now?.progress(nowMs),
                            playing = here && index == playingIndex,
                            onVideo = true,
                            modifier = focus,
                            onClick = { onPick(state.group, index) },
                        )
                    }
                }
            }
        }
    }
}
