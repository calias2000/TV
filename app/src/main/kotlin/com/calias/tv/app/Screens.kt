package com.calias.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.core.Channel
import com.calias.tv.core.library.Category

/** Home: three large tiles, plus Sync and Settings. Tiles stay dimmed until a playlist is loaded. */
@Composable
fun HomeScreen(state: PlaylistState, onOpen: (Any) -> Unit, onSync: () -> Unit) {
    val ready = state is PlaylistState.Loaded
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(text = "TV", style = MaterialTheme.typography.headlineLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HomeTile(label = "Live TV", ready = ready) { onOpen(LiveTv) }
            HomeTile(label = "Movies", ready = ready) { onOpen(MovieList) }
            HomeTile(label = "Series", ready = ready) { onOpen(SeriesList) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onSync) { Text(text = "Sync") }
            Button(onClick = { onOpen(Settings) }) { Text(text = "Settings") }
        }
        StatusLine(state)
    }
}

/**
 * A large home tile. When not ready it is dimmed and cannot take focus, so pressing it does
 * nothing and the remote skips it.
 */
@Composable
private fun HomeTile(label: String, ready: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .alpha(if (ready) 1f else 0.35f)
            .focusProperties { canFocus = ready },
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** One line on Home and Settings: what the playlist state is right now. */
@Composable
private fun StatusLine(state: PlaylistState) {
    val text = when (state) {
        PlaylistState.Loading -> "Loading playlist..."
        is PlaylistState.Loaded -> "Playlist loaded: ${state.playlist.channels.size} entries"
        is PlaylistState.Failed -> "Playlist unavailable: ${state.message}"
    }
    Text(text = text)
}

/** Live TV side by side: categories on the left, the channels of the selected category on the right. */
@Composable
fun LiveScreen(categories: List<Category>, onWatch: (category: String, channelId: String) -> Unit) {
    if (categories.isEmpty()) {
        Text(text = "No live channels", modifier = Modifier.padding(48.dp))
        return
    }
    var selected by remember { mutableIntStateOf(0) }
    Row(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        LazyColumn(modifier = Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(categories) { index, category ->
                Card(
                    onClick = { selected = index },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${category.name} (${category.channels.size})",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        val current = categories[selected]
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(current.channels, key = { it.id }) { channel ->
                ChannelRow(channel) { onWatch(current.name, channel.id) }
            }
        }
    }
}

/** A flat list of movies or series episodes. */
@Composable
fun ChannelListScreen(title: String, channels: List<Channel>, onWatch: (channelId: String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (channels.isEmpty()) {
            Text(text = "Nothing to show. Check Settings.", modifier = Modifier.padding(top = 24.dp))
            return@Column
        }
        LazyColumn(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelRow(channel) { onWatch(channel.id) }
            }
        }
    }
}

/** Settings: the playlist source, its status, and a sync button. */
@Composable
fun SettingsScreen(state: PlaylistState, onSync: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Playlist source: ${PlaylistLoader.PLAYLIST_URL}")
        StatusLine(state)
        Button(onClick = onSync) { Text(text = "Sync now") }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = channel.name, modifier = Modifier.padding(16.dp))
    }
}
