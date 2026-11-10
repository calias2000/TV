package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ChannelLogo
import com.calias.tv.app.ui.Corner
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassSurface
import com.calias.tv.app.ui.ProgressBar
import com.calias.tv.app.ui.Space
import com.calias.tv.core.Channel

/**
 * The one row used wherever the app lists groups, channels, movies or episodes, so every list looks
 * and behaves the same. Every playlist entry is its own row, even when names repeat.
 *
 * - [channel]: shows its logo (or poster) before the text.
 * - [subtitle] and [progress]: what is on now and how far along it is (live, from the guide).
 * - [trailing]: a muted note at the end of the row, such as the size of a group.
 * - [playing]: null leaves out the marker column; true or false adds it, with a ▶ when true.
 * - [onVideo]: for rows on the panels over the video: light on dark in either theme.
 * A row is at least as tall as a button, with or without a logo. No focus zoom, so rows stay aligned and never spill past the margin.
 */
@Composable
fun ChannelRow(
    title: String,
    modifier: Modifier = Modifier,
    channel: Channel? = null,
    subtitle: String? = null,
    progress: Float? = null,
    trailing: String? = null,
    playing: Boolean? = null,
    onVideo: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = if (onVideo) Glass.overVideo else Glass.colors
    GlassSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = Space.Control),
        shape = Corner.Medium,
        colors = colors,
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Fixed-width marker column, so titles line up on every row.
            if (playing != null) Text(text = if (playing) "▶" else "", modifier = Modifier.width(18.dp))
            if (channel != null) ChannelLogo(channel)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, maxLines = if (subtitle == null) 2 else 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (progress != null) ProgressBar(progress, Modifier.padding(top = 3.dp))
            }
            if (trailing != null) Text(text = trailing, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
    }
}
