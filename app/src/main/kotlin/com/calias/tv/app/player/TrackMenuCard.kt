package com.calias.tv.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.Space
import com.calias.tv.app.ui.SubtitleLine
import com.calias.tv.app.ui.SubtitleSize

/** One row of the card: a track (or "Off"), whether it plays now, whether this TV can play it, and what OK does. */
private class TrackRow(val label: String, val selected: Boolean, val supported: Boolean, val onPick: () -> Unit)

/**
 * The glass card on the right with the audio or subtitle tracks of the file ([menu]). OK on a row plays
 * it, then [onPicked] closes the card. A track this TV cannot play is listed but does nothing. Like the
 * browse panels it takes focus for its rows, and the screen takes it back when it closes.
 * The subtitles card also has the text size: a sample line, and a first row where LEFT/RIGHT (or OK) change
 * it without closing the card.
 */
@Composable
fun TrackMenuCard(
    visible: Boolean,
    menu: TrackMenu,
    tracks: TracksState,
    subtitleSize: SubtitleSize,
    onSubtitleSize: (SubtitleSize) -> Unit,
    onPicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxHeight().padding(Edge.Margin),
        enter = slideInHorizontally { it / 8 } + fadeIn(),
        exit = slideOutHorizontally { it / 8 } + fadeOut(),
    ) {
        val rows = when (menu) {
            TrackMenu.AUDIO -> tracks.audio.map { TrackRow(it.label, it.selected, it.supported) { tracks.select(it) } }
            TrackMenu.SUBTITLES ->
                listOf(TrackRow("Off", !tracks.subtitlesOn, true, tracks::hideSubtitles)) +
                    tracks.text.map { TrackRow(it.label, it.selected, it.supported) { tracks.select(it) } }
        }
        GlassPanel(modifier = Modifier.width(340.dp).fillMaxHeight()) {
            Column(modifier = Modifier.padding(horizontal = Space.Inset).padding(top = Space.Gap)) {
                PanelTitle(menu.label)
                val isSubtitles = menu == TrackMenu.SUBTITLES
                if (isSubtitles) SubtitleLine(text = "This is how subtitles look.", size = subtitleSize, modifier = Modifier.padding(Space.Inset))
                if (rows.isEmpty()) {
                    Text(text = "Nothing to choose", color = Glass.overVideo.textMuted, modifier = Modifier.padding(Space.Inset))
                } else {
                    // The subtitles card has the size row before the tracks.
                    val first = if (isSubtitles) 1 else 0
                    BrowseList(count = rows.size + first, focusIndex = rows.indexOfFirst { it.selected }.coerceAtLeast(0) + first) { index, focus ->
                        if (index < first) {
                            SizeRow(subtitleSize, onSubtitleSize, focus)
                            return@BrowseList
                        }
                        val row = rows[index - first]
                        ChannelRow(
                            title = row.label,
                            trailing = if (row.selected) "✓" else if (!row.supported) "Not supported" else null,
                            onVideo = true,
                            modifier = focus,
                            onClick = {
                                if (row.supported) {
                                    row.onPick()
                                    onPicked()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The text size as a row: LEFT and RIGHT step through the sizes, OK goes to the next one and back to the first. */
@Composable
private fun SizeRow(size: SubtitleSize, onSize: (SubtitleSize) -> Unit, modifier: Modifier) {
    val sizes = SubtitleSize.entries
    ChannelRow(
        title = "Text size",
        trailing = size.label,
        onVideo = true,
        modifier = modifier.onPreviewKeyEvent { event ->
            val step = when {
                event.type != KeyEventType.KeyDown -> return@onPreviewKeyEvent false
                event.key == Key.DirectionLeft -> -1
                event.key == Key.DirectionRight -> 1
                else -> return@onPreviewKeyEvent false
            }
            onSize(sizes[(size.ordinal + step).coerceIn(0, sizes.lastIndex)])
            true
        },
        onClick = { onSize(sizes[(size.ordinal + 1) % sizes.size]) },
    )
}
