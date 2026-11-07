package com.calias.tv.app.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category

// Where the mini player sits. The video itself is drawn by the player screen (one player, one video
// view, so going full screen does not reload anything); this screen leaves the same space free.
private val MiniWidth = 340.dp
private val MiniHeight = MiniWidth * 9 / 16
private val EdgeX = 32.dp
private val EdgeY = 24.dp

/**
 * The frame the video is drawn in: the mini player corner while [mini], else the whole screen. It is
 * one box whose size changes, so the video view stays the same one and nothing reloads.
 */
@Composable
fun BoxScope.VideoFrame(mini: Boolean, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = if (mini) {
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = EdgeY, end = EdgeX)
                .size(MiniWidth, MiniHeight)
                .background(Color.Black)
        } else {
            Modifier.fillMaxSize()
        },
        content = content,
    )
}

/**
 * The Live screen: groups on the left, the channels of the browsed group in the middle, and on the
 * right the mini player (drawn by the player screen) with the guide of the channel under the cursor.
 *
 * Moving only browses: nothing starts playing. OK on a channel plays it in the mini player; OK on the
 * channel that already plays goes full screen ([onOpen] tells which one was pressed). Moving onto a
 * group shows its channels. LEFT from the channels goes back to the browsed group.
 * When the screen opens, focus is on the playing channel.
 */
@Composable
fun LiveBrowse(
    groups: List<Category>,
    playingGroup: Int,
    playingIndex: Int,
    started: Boolean,
    guide: Guide,
    nowMs: Long,
    onOpen: (group: Int, index: Int) -> Unit,
) {
    var browsed by remember { mutableIntStateOf(playingGroup) }
    var focusMoved by remember { mutableStateOf(false) } // false until the user moves to a group
    var underCursor by remember { mutableStateOf<Channel?>(null) }
    val groupEntry = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = EdgeX, vertical = EdgeY),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrowseList(
            count = groups.size,
            focusIndex = browsed,
            modifier = Modifier.width(220.dp),
            autoFocus = false,
            focusRequester = groupEntry,
        ) { index, focus ->
            ChannelRow(
                title = "${groups[index].name} (${groups[index].channels.size})",
                playing = index == playingGroup,
                modifier = focus.onFocusChanged {
                    if (it.hasFocus) {
                        browsed = index
                        focusMoved = true
                    }
                },
                onClick = { focusManager.moveFocus(FocusDirection.Right) },
            )
        }

        val channels = groups[browsed].channels
        // A new list (starting at the playing channel) whenever the browsed group changes.
        composeKey(browsed) {
            val here = started && browsed == playingGroup
            BrowseList(
                count = channels.size,
                focusIndex = if (here) playingIndex else 0,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        // LEFT goes to the browsed group, not to whichever group row is nearest.
                        val left = event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft
                        if (left) groupEntry.requestFocus()
                        left
                    },
                autoFocus = !focusMoved,
            ) { index, focus ->
                val channel = channels[index]
                val nowNext = guide.nowNext(channel.tvgId, nowMs)?.now
                ChannelRow(
                    title = channel.name,
                    channel = channel,
                    subtitle = nowNext?.title,
                    progress = nowNext?.progress(nowMs),
                    playing = here && index == playingIndex,
                    modifier = focus.onFocusChanged { if (it.hasFocus) underCursor = channel },
                    onClick = { onOpen(browsed, index) },
                )
            }
        }

        Column(modifier = Modifier.width(MiniWidth)) {
            Spacer(modifier = Modifier.height(MiniHeight + 16.dp)) // the mini player
            val shown = underCursor ?: groups[playingGroup].channels[playingIndex]
            ProgramInfo(shown.name, guide.schedule(shown.tvgId, nowMs), nowMs)
        }
    }
}

/**
 * Text on the mini player: the playing channel's name on the picture, or why nothing shows. Fixed
 * white-on-dark colors, it sits on the video.
 */
@Composable
fun MiniPlayerOverlay(name: String, started: Boolean, buffering: Boolean, failed: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        val status = when {
            !started -> "Press OK on a channel"
            failed -> "Playback failed. OK for full screen"
            buffering -> "Buffering…"
            else -> null
        }
        if (status != null) {
            Text(text = status, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp))
        }
        if (started) {
            Text(
                text = name,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
