package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.player.PlayerScreen
import com.calias.tv.core.Channel

/** Entry screen. Lists the channels; selecting one opens the player. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: PlaylistViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                // Index into the channel list. Null = list screen; a value = player on that channel.
                var selectedIndex by remember { mutableStateOf<Int?>(null) }
                val channels = (state as? PlaylistState.Loaded)?.playlist?.channels.orEmpty()

                val index = selectedIndex
                if (index != null && channels.isNotEmpty()) {
                    PlayerScreen(
                        channels = channels,
                        index = index,
                        onIndexChange = { selectedIndex = it },
                        onExit = { selectedIndex = null },
                    )
                } else {
                    PlaylistScreen(
                        state = state,
                        onChannelClick = { channel -> selectedIndex = channels.indexOf(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistScreen(state: PlaylistState, onChannelClick: (Channel) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
    ) {
        Text(
            text = "Ola Mami, eu amo-te muito!",
            style = MaterialTheme.typography.headlineMedium,
        )
        when (state) {
            PlaylistState.Loading -> Text(
                text = "Loading playlist...",
                modifier = Modifier.padding(top = 24.dp),
            )
            is PlaylistState.Failed -> Text(
                text = "Could not load playlist: ${state.message}",
                modifier = Modifier.padding(top = 24.dp),
            )
            is PlaylistState.Loaded -> LazyColumn(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.playlist.channels, key = { it.id }) { channel ->
                    ChannelRow(channel, onClick = { onChannelClick(channel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = channel.name,
            modifier = Modifier.padding(16.dp),
        )
    }
}
