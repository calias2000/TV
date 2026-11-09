package com.calias.tv.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.calias.tv.app.player.PlayerScreen
import com.calias.tv.core.ContentKind
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.Edge
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Category
import com.calias.tv.core.library.Library
import com.calias.tv.core.recents.Recents
import com.calias.tv.core.resume.Resume

// Destinations. Each object or data class is one key on the back stack.
data object Home
/** Live TV. [channelUrl]: the channel to open on (null: none, the user picks). */
data class LiveTv(val channelUrl: String? = null)
data object MovieList
data object SeriesList
/** The episodes of the show called [name]. */
data class ShowEpisodes(val name: String)
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
    onToggleTheme: () -> Unit,
    font: AppFont,
    onFont: (AppFont) -> Unit,
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
                // Collected here, not above: a saved position must only redraw Home, not every screen.
                val resumeRecords by prefs.resume.collectAsStateWithLifecycle()
                val resume = remember(resumeRecords, playlist) {
                    playlist?.let { Resume.continueWatching(resumeRecords, it.channels) }.orEmpty()
                }
                val recentUrls by prefs.recentLive.collectAsStateWithLifecycle()
                val recentLive = remember(recentUrls, playlist) {
                    playlist?.let { Recents.resolve(recentUrls, it.channels) }.orEmpty()
                }
                HomeScreen(
                    state = state,
                    counts = playlist?.let { Library.counts(it.channels) },
                    resume = resume,
                    recentLive = recentLive,
                    guide = guide,
                    onResume = { open(Watch(it.channel.kind, it.channel.id)) },
                    onRecentLive = { open(LiveTv(it.streamUrl)) },
                    onOpen = open,
                    onSync = onSync,
                    onToggleTheme = onToggleTheme,
                )
            }
            entry<LiveTv> { key ->
                val groups = remember(playlist) { playlist?.let { Library.liveCategories(it.channels) }.orEmpty() }
                if (groups.isEmpty()) {
                    Text(text = "No live channels", modifier = Modifier.padding(Edge.Margin))
                } else {
                    // Opens on the channel Home asked for (the last one played) and plays it. Without one,
                    // nothing plays until OK.
                    val start = remember(groups, key) { Library.locate(groups, key.channelUrl) }
                    PlayerScreen(
                        groups = groups,
                        initialGroup = start?.group ?: 0,
                        initialIndex = start?.index ?: 0,
                        isLive = true,
                        startPlaying = start != null,
                        guide = guide,
                        onTuned = prefs::saveRecentLive,
                        onExit = back,
                    )
                }
            }
            entry<MovieList> {
                val movies = remember(playlist) { playlist?.let { Library.movies(it.channels) }.orEmpty() }
                val resume by prefs.resume.collectAsStateWithLifecycle()
                MoviesScreen(movies = movies, resume = resume, onWatch = { open(Watch(ContentKind.MOVIE, it)) })
            }
            entry<SeriesList> {
                val shows = remember(playlist) { playlist?.let { Library.shows(it.channels) }.orEmpty() }
                ShowsScreen(shows = shows, onOpen = { open(ShowEpisodes(it.name)) })
            }
            entry<ShowEpisodes> { key ->
                val show = remember(playlist, key) { playlist?.let { Library.shows(it.channels) }?.firstOrNull { it.name == key.name } }
                val resume by prefs.resume.collectAsStateWithLifecycle()
                if (show != null) EpisodesScreen(show = show, resume = resume, onWatch = { open(Watch(ContentKind.SERIES, it)) })
            }
            entry<Settings> {
                SettingsScreen(state = state, onSync = onSync, font = font, onFont = onFont)
            }
            entry<Watch> { key ->
                val groups = remember(playlist, key.kind) {
                    val channels = playlist?.channels.orEmpty().filter { it.kind == key.kind }
                    listOf(Category(if (key.kind == ContentKind.MOVIE) "Movies" else "Series", channels))
                }
                val index = remember(groups, key.channelId) { groups[0].channels.indexOfFirst { it.id == key.channelId } }
                if (index >= 0) {
                    PlayerScreen(
                        groups = groups,
                        initialGroup = 0,
                        initialIndex = index,
                        isLive = false,
                        startPlaying = true,
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
