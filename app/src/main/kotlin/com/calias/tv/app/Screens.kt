package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.GlassButton
import com.calias.tv.app.ui.Spinner
import com.calias.tv.core.Channel

/** What the playlist state says, for Settings and for Home's status pill. Null while loading: a spinner shows then. */
internal fun statusText(state: PlaylistState): String? = when (state) {
    PlaylistState.Loading -> null
    is PlaylistState.Loaded -> "Playlist loaded: ${state.playlist.channels.size} entries"
    is PlaylistState.Failed -> "Playlist unavailable: ${state.message}. Press Sync to retry."
}

/** A flat list of movies or series episodes. */
@Composable
fun ChannelListScreen(title: String, channels: List<Channel>, onWatch: (channelId: String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(Edge.Margin)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (channels.isEmpty()) {
            Text(text = "Nothing to show. Check Settings.", modifier = Modifier.padding(top = 24.dp))
            return@Column
        }
        BrowseList(count = channels.size, focusIndex = 0) { index, focus ->
            val channel = channels[index]
            ChannelRow(title = channel.name, channel = channel, modifier = focus, onClick = { onWatch(channel.id) })
        }
    }
}

/** Settings: the playlist source, its status, a sync button, and the font. */
@Composable
fun SettingsScreen(state: PlaylistState, onSync: () -> Unit, font: AppFont, onFont: (AppFont) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Edge.Margin),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Playlist source: ${PlaylistLoader.PLAYLIST_URL}")
        statusText(state)?.let { Text(text = it) } ?: Spinner(diameter = 24.dp)
        GlassButton(text = "Sync now", onClick = onSync)
        Text(text = "Font", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppFont.entries.forEach { option ->
                GlassButton(
                    text = if (option == font) "✓ ${option.label}" else option.label,
                    onClick = { onFont(option) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = option.family),
                )
            }
        }
    }
}
