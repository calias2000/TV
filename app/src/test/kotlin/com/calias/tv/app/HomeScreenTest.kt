package com.calias.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.update.Release
import com.calias.tv.app.update.UpdateState
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.epg.Program
import com.calias.tv.core.library.Counts
import com.calias.tv.core.playlist.Playlist
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    private var updates = 0

    private fun home(
        state: PlaylistState,
        counts: Counts?,
        recentLive: List<Channel> = listOf(recent),
        guide: Guide = Guide.EMPTY,
        update: UpdateState = UpdateState.Idle,
    ) = compose.setContent {
        AppTheme(dark = true, font = AppFont.SYSTEM) {
            HomeScreen(
                state = state,
                counts = counts,
                resume = emptyList(),
                recentLive = recentLive,
                guide = guide,
                onResume = {},
                onRecentLive = {},
                onOpen = { opened = it },
                onProfiles = { profilesOpened++ },
                onSync = {},
                onToggleTheme = {},
                update = update,
                onUpdate = { updates++ },
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
    fun `recent cards are as tall as each other, whatever the name and whatever is on now`() {
        val long = Channel("c2", "A very long channel name that would need three lines on a card this wide", "http://x/c2", group = null)
        val onNow = Channel("c3", "With Guide", "http://x/c3", group = null, tvgId = "tv3")
        val now = System.currentTimeMillis()
        val guide = Guide(mapOf("tv3" to listOf(Program(now - 600_000, now + 600_000, "The News"))))
        home(loaded, Counts(3, 0, 0), recentLive = listOf(recent, long, onNow), guide = guide)

        fun height(name: String) = compose.onAllNodes(hasClickAction() and hasText(name))[0].fetchSemanticsNode().size.height
        assertEquals(height("Channel One"), height(long.name))
        assertEquals(height("Channel One"), height(onNow.name))
        // As tall as a logo and two lines need, not a poster-sized box: the content stays centered in it.
        assertTrue(height("Channel One") < with(compose.density) { 80.dp.roundToPx() })
    }

    @Test
    fun `an update that was found has a button on Home, and OK on it asks for the update`() {
        home(loaded, Counts(1, 0, 0), update = UpdateState.Available(Release("0.2.0", "http://x/app.apk", 10, null)))
        compose.onNodeWithText("Update to 0.2.0").focusAndPressOk(compose)
        assertEquals(1, updates)
    }

    @Test
    fun `with no update there is no update button`() {
        home(loaded, Counts(1, 0, 0), update = UpdateState.UpToDate)
        assertTrue(compose.onAllNodesWithText("Update", substring = true).fetchSemanticsNodes().isEmpty())
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
