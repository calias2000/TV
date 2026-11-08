package com.calias.tv.app.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.calias.tv.app.ChannelLogo
import com.calias.tv.app.ui.Glass
import com.calias.tv.core.Channel

/**
 * The one row used wherever the app lists groups, channels, movies or episodes, so every list
 * looks and behaves the same. Every playlist entry is its own row, even when names repeat.
 *
 * - [channel]: shows its logo (or poster) before the text.
 * - [subtitle] and [progress]: what is on now and how far along it is (live, from the guide).
 * - [playing]: null leaves out the marker column; true or false adds it, with a ▶ when true.
 * - [onVideo]: fixed white-on-dark colors for rows drawn over the video; otherwise theme colors.
 * No focus zoom, so rows stay aligned and never spill past the margin.
 */
@Composable
fun ChannelRow(
    title: String,
    modifier: Modifier = Modifier,
    channel: Channel? = null,
    subtitle: String? = null,
    progress: Float? = null,
    playing: Boolean? = null,
    onVideo: Boolean = false,
    onClick: () -> Unit,
) {
    if (onVideo) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.22f),
                focusedContentColor = Color.White,
                pressedContainerColor = Color.White.copy(alpha = 0.3f),
                pressedContentColor = Color.White,
            ),
        ) {
            RowContent(title, channel, subtitle, progress, playing)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            scale = CardDefaults.scale(focusedScale = 1f),
        ) {
            RowContent(title, channel, subtitle, progress, playing)
        }
    }
}

@Composable
private fun RowContent(title: String, channel: Channel?, subtitle: String?, progress: Float?, playing: Boolean?) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed-width marker column, so titles line up on every row.
        if (playing != null) Text(text = if (playing) "▶" else "", modifier = Modifier.width(24.dp))
        if (channel != null) ChannelLogo(channel)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, maxLines = 2)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            if (progress != null) ProgressBar(progress, Modifier.padding(top = 4.dp))
        }
    }
}

/** A thin bar showing [fraction] (0 to 1) of the way, in the theme accent. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(Color.Gray.copy(alpha = 0.4f))) {
        Box(modifier = Modifier.fillMaxWidth(fraction).height(3.dp).background(Glass.colors.accent))
    }
}
