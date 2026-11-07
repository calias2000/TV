package com.calias.tv.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.calias.tv.app.player.PlayerScreen
import com.calias.tv.core.ContentKind
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import com.calias.tv.core.library.Library
import com.calias.tv.core.resume.Resume

// Destinations. Each object or data class is one key on the back stack.
data object Home
data object LiveTv
data object MovieList
data object SeriesList
data object Settings
/** A movie or episode ([kind]). Playback steps through the list of that kind (CH+/CH-). Live TV has its own destination. */
data class Watch(val kind: ContentKind, val channelId: String)

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
    val resumeRecords by prefs.resume.collectAsStateWithLifecycle()

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<Home> {
                val continueItems = remember(resumeRecords, playlist) {
                    playlist?.let { Resume.continueWatching(resumeRecords, it.channels) }.orEmpty()
                }
                HomeScreen(
                    state = state,
                    dark = dark,
                    continueItems = continueItems,
                    onContinue = { item ->
                        open(Watch(item.channel.kind, item.channel.id))
                    },
                    onOpen = open,
                    onSync = onSync,
                    onToggleTheme = onToggleTheme,
                )
            }
            entry<LiveTv> {
                val groups = remember(playlist) { playlist?.let { Library.liveCategories(it.channels) }.orEmpty() }
                if (groups.isEmpty()) {
                    Text(text = "No live channels", modifier = Modifier.padding(48.dp))
                } else {
                    // Opens on the last live channel, playing. Without one, nothing plays until OK.
                    // Read once when the screen opens: tuning saves a new value, which must not move the screen.
                    val lastUrl by prefs.lastLive.collectAsStateWithLifecycle()
                    val last = remember(groups) { Library.locate(groups, lastUrl) }
                    PlayerScreen(
                        groups = groups,
                        initialGroup = last?.group ?: 0,
                        initialIndex = last?.index ?: 0,
                        isLive = true,
                        startPlaying = last != null,
                        guide = guide,
                        startPosition = prefs::startPosition,
                        onProgress = prefs::saveProgress,
                        onTuned = prefs::saveLastLive,
                        onExit = back,
                    )
                }
            }
            entry<MovieList> {
                ChannelListScreen(
                    title = "Movies",
                    channels = playlist?.let { Library.movies(it.channels) }.orEmpty(),
                    onWatch = { open(Watch(ContentKind.MOVIE, it)) },
                )
            }
            entry<SeriesList> {
                ChannelListScreen(
                    title = "Series",
                    channels = playlist?.let { Library.series(it.channels) }.orEmpty(),
                    onWatch = { open(Watch(ContentKind.SERIES, it)) },
                )
            }
            entry<Settings> {
                SettingsScreen(state = state, onSync = onSync)
            }
            entry<Watch> { key ->
                val isMovie = key.kind == ContentKind.MOVIE
                val channels = playlist?.let { if (isMovie) Library.movies(it.channels) else Library.series(it.channels) }.orEmpty()
                val index = channels.indexOfFirst { it.id == key.channelId }
                if (index >= 0) {
                    PlayerScreen(
                        groups = listOf(Category(if (isMovie) "Movies" else "Series", channels)),
                        initialGroup = 0,
                        initialIndex = index,
                        isLive = false,
                        startPlaying = true,
                        guide = guide,
                        startPosition = prefs::startPosition,
                        onProgress = prefs::saveProgress,
                        onTuned = {},
                        onExit = back,
                    )
                }
            }
        },
    )
}
