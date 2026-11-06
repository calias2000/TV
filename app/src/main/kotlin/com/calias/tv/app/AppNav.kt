package com.calias.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.calias.tv.app.player.PlayerScreen
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
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

/** The groups the sidebar can browse for a [PlaySource]: all live categories, or one flat list. */
internal fun PlaySource.groups(playlist: Playlist): List<Category> = when (this) {
    is PlaySource.LiveCategory -> Library.liveCategories(playlist.channels)
    PlaySource.Movies -> listOf(Category("Movies", Library.movies(playlist.channels)))
    PlaySource.Series -> listOf(Category("Series", Library.series(playlist.channels)))
}

/** Which of [groups] this source started in. */
internal fun PlaySource.groupIndex(groups: List<Category>): Int = when (this) {
    is PlaySource.LiveCategory -> groups.indexOfFirst { it.name == name }
    else -> 0
}

/**
 * The app's navigation. We own the back stack (Navigation 3): opening a screen pushes its key,
 * BACK pops it. Home is the bottom of the stack and cannot be popped.
 */
@Composable
fun AppNav(
    state: PlaylistState,
    guide: Guide,
    onSync: () -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    prefs: PrefsViewModel,
) {
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
                    guide = guide,
                    onWatch = { category, channelId ->
                        open(Watch(PlaySource.LiveCategory(category), channelId))
                    },
                )
            }
            entry<MovieList> {
                ChannelListScreen(
                    title = "Movies",
                    channels = playlist?.let { Library.movies(it.channels) }.orEmpty(),
                    onWatch = { open(Watch(PlaySource.Movies, it)) },
                )
            }
            entry<SeriesList> {
                ChannelListScreen(
                    title = "Series",
                    channels = playlist?.let { Library.series(it.channels) }.orEmpty(),
                    onWatch = { open(Watch(PlaySource.Series, it)) },
                )
            }
            entry<Settings> {
                SettingsScreen(state = state, onSync = onSync)
            }
            entry<Watch> { key ->
                val groups = playlist?.let { key.source.groups(it) }.orEmpty()
                val group = key.source.groupIndex(groups)
                val index = groups.getOrNull(group)?.channels?.indexOfFirst { it.id == key.channelId } ?: -1
                if (index >= 0) {
                    PlayerScreen(
                        groups = groups,
                        initialGroup = group,
                        initialIndex = index,
                        isLive = key.source is PlaySource.LiveCategory,
                        guide = guide,
                        startPosition = prefs::startPosition,
                        onProgress = prefs::saveProgress,
                        onExit = back,
                    )
                }
            }
        },
    )
}
