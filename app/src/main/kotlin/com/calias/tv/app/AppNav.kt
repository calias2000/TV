package com.calias.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.calias.tv.app.player.PlayerScreen
import com.calias.tv.core.Channel
import com.calias.tv.core.library.Library
import com.calias.tv.core.playlist.Playlist

/**
 * Where a playing channel came from. Playback steps through this list (CH+/CH-), so the list is
 * part of the destination, not of the player.
 */
sealed interface PlaySource {
    data class LiveCategory(val name: String) : PlaySource
    data object Movies : PlaySource
    data object Series : PlaySource
}

// Destinations. Each object or data class is one key on the back stack.
data object Home
data object LiveTv
data object MovieList
data object SeriesList
data object Settings
data class Watch(val source: PlaySource, val channelId: String)

/** The channels a [PlaySource] refers to, in the order they are browsed. */
internal fun PlaySource.channels(playlist: Playlist): List<Channel> = when (this) {
    is PlaySource.LiveCategory ->
        Library.liveCategories(playlist.channels).firstOrNull { it.name == name }?.channels.orEmpty()
    PlaySource.Movies -> Library.movies(playlist.channels)
    PlaySource.Series -> Library.series(playlist.channels)
}

/**
 * The app's navigation. We own the back stack (Navigation 3): opening a screen pushes its key,
 * BACK pops it. Home is the bottom of the stack and cannot be popped.
 */
@Composable
fun AppNav(state: PlaylistState, onSync: () -> Unit, dark: Boolean, onToggleTheme: () -> Unit) {
    val backStack = remember { mutableStateListOf<Any>(Home) }
    val open: (Any) -> Unit = { backStack.add(it) }
    val back: () -> Unit = { backStack.removeLastOrNull() }
    val playlist = (state as? PlaylistState.Loaded)?.playlist

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    state = state,
                    dark = dark,
                    onOpen = open,
                    onSync = onSync,
                    onToggleTheme = onToggleTheme,
                )
            }
            entry<LiveTv> {
                LiveScreen(
                    categories = playlist?.let { Library.liveCategories(it.channels) }.orEmpty(),
                    onWatch = { category, channelId ->
                        open(Watch(PlaySource.LiveCategory(category), channelId))
                    },
                )
            }
            entry<MovieList> {
                ChannelListScreen(
                    title = "Movies",
                    channels = playlist?.let { PlaySource.Movies.channels(it) }.orEmpty(),
                    onWatch = { open(Watch(PlaySource.Movies, it)) },
                )
            }
            entry<SeriesList> {
                ChannelListScreen(
                    title = "Series",
                    channels = playlist?.let { PlaySource.Series.channels(it) }.orEmpty(),
                    onWatch = { open(Watch(PlaySource.Series, it)) },
                )
            }
            entry<Settings> {
                SettingsScreen(state = state, onSync = onSync)
            }
            entry<Watch> { key ->
                val list = playlist?.let { key.source.channels(it) }.orEmpty()
                val index = list.indexOfFirst { it.id == key.channelId }
                if (index >= 0) {
                    PlayerScreen(channels = list, initialIndex = index, onExit = back)
                }
            }
        },
    )
}
