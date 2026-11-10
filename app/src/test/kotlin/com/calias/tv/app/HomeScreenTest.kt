package com.calias.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Counts
import com.calias.tv.core.playlist.Playlist
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Home screen with a made-up playlist: tiles, counts, the row of recent channels, and the keys. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val recent = Channel("c1", "Channel One", "http://x/c1", group = null)
    private val loaded = PlaylistState.Loaded(Playlist(epgUrl = null, channels = listOf(recent)))
    private var opened: Any? = null
    private var profilesOpened = 0

    private fun home(state: PlaylistState, counts: Counts?, recentLive: List<Channel> = listOf(recent)) = compose.setContent {
        AppTheme(dark = true, font = AppFont.SYSTEM) {
            HomeScreen(
                state = state,
                counts = counts,
                resume = emptyList(),
                recentLive = recentLive,
                guide = Guide.EMPTY,
                onResume = {},
                onRecentLive = {},
                onOpen = { opened = it },
                onProfiles = { profilesOpened++ },
                onSync = {},
                onToggleTheme = {},
            )
        }
    }

    @Test
    fun `the tiles show what the playlist holds`() {
        home(loaded, Counts(live = 444, movies = 1, episodes = 25))
        compose.onNodeWithText("Live TV").assertIsDisplayed()
        compose.onNodeWithText("444 channels").assertIsDisplayed()
        compose.onNodeWithText("1 movie").assertIsDisplayed()
        compose.onNodeWithText("25 episodes").assertIsDisplayed()
    }

    @Test
    fun `Live TV has focus first and opens on the channel watched last`() {
        home(loaded, Counts(1, 0, 0))
        compose.press(Key.DirectionCenter)
        assertEquals(LiveTv("http://x/c1"), opened)
    }

    @Test
    fun `RIGHT moves to Movies and then Series`() {
        home(loaded, Counts(1, 1, 1))
        compose.press(Key.DirectionRight)
        compose.press(Key.DirectionCenter)
        assertEquals(MovieList, opened)
        compose.press(Key.DirectionRight)
        compose.press(Key.DirectionCenter)
        assertEquals(SeriesList, opened)
    }

    @Test
    fun `the profile button asks for the list of profiles`() {
        home(loaded, Counts(1, 0, 0))
        compose.onNodeWithContentDescription("Profiles").focusAndPressOk(compose)
        assertEquals(1, profilesOpened)
    }

    @Test
    fun `the row under the tiles lists the recent channels`() {
        home(loaded, Counts(1, 0, 0))
        compose.onNodeWithText("Recently watched").assertIsDisplayed()
        compose.onNodeWithText("Channel One").assertIsDisplayed()
    }

    @Test
    fun `while the playlist is unavailable the tiles do nothing and the reason shows`() {
        home(PlaylistState.Failed("no network"), counts = null)
        compose.onNodeWithText("Playlist unavailable: no network. Press Sync to retry.").assertIsDisplayed()
        compose.press(Key.DirectionCenter)
        assertNull(opened)
    }
}
