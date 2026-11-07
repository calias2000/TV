package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.core.Channel
import com.calias.tv.core.resume.ContinueItem
import com.calias.tv.core.seek.Seek

/**
 * Home: the title with Sync, Settings and the theme on top, three large tiles, and a "Continue
 * watching" row when there is something to continue. Everything has to fit a 540 dp tall TV screen.
 * Tiles stay dimmed until a playlist is loaded; when it loads, focus moves to the tile that was used
 * last (the first one on a fresh start).
 */
@Composable
fun HomeScreen(
    state: PlaylistState,
    dark: Boolean,
    continueItems: List<ContinueItem>,
    onContinue: (ContinueItem) -> Unit,
    onOpen: (Any) -> Unit,
    onSync: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val ready = state is PlaylistState.Loaded
    val tiles = listOf("Live TV" to LiveTv, "Movies" to MovieList, "Series" to SeriesList)
    val tileFocus = remember { List(tiles.size) { FocusRequester() } }
    var lastTile by rememberSaveable { mutableIntStateOf(0) }
    // The tiles cannot take focus while loading, so focus waits for the playlist.
    LaunchedEffect(ready) { if (ready) tileFocus[lastTile].requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "TV", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            Button(onClick = onSync) { Text(text = "Sync") }
            Button(onClick = { onOpen(Settings) }) { Text(text = "Settings") }
            Button(onClick = onToggleTheme) { Text(text = if (dark) "Theme: Dark" else "Theme: Light") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            tiles.forEachIndexed { index, (label, destination) ->
                HomeTile(label = label, ready = ready, focusRequester = tileFocus[index]) {
                    lastTile = index
                    onOpen(destination)
                }
            }
        }
        if (continueItems.isNotEmpty()) {
            Text(text = "Continue watching", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(continueItems, key = { it.channel.id }) { item -> ContinueCard(item) { onContinue(item) } }
            }
        }
        StatusLine(state)
    }
}

/**
 * A large home tile. When not ready it is dimmed and cannot take focus, so pressing it does
 * nothing and the remote skips it.
 */
@Composable
private fun HomeTile(label: String, ready: Boolean, focusRequester: FocusRequester, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .width(300.dp)
            .height(160.dp)
            .alpha(if (ready) 1f else 0.35f)
            .focusProperties { canFocus = ready },
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** A movie or episode to continue: its picture, title, and where it will resume. */
@Composable
private fun ContinueCard(item: ContinueItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(460.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogo(item.channel)
            Column {
                Text(text = item.channel.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "Resume at ${Seek.formatClock(item.positionMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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

/** A flat list of movies or series episodes. */
@Composable
fun ChannelListScreen(title: String, channels: List<Channel>, onWatch: (channelId: String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
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
