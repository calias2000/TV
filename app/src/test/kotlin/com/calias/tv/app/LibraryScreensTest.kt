package com.calias.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import com.calias.tv.core.series.Show
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Movies, Series and episode screens, drawn with made-up channels: no network, no files. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class LibraryScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun movie(id: String, name: String) = Channel(id, name, "http://x/$id", group = null, kind = ContentKind.MOVIE)
    private fun episode(id: String, name: String) = Channel(id, name, "http://x/$id", group = null, kind = ContentKind.SERIES)

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        compose.setContent { AppTheme(dark = true, font = AppFont.SYSTEM, content = content) }

    @Test
    fun `movies show their titles and the count, the first has focus, and OK opens the one in focus`() {
        var opened: String? = null
        show {
            MoviesScreen(
                movies = listOf(movie("m1", "Alpha"), movie("m2", "Beta")),
                resume = emptyMap(),
                onWatch = { opened = it },
            )
        }
        compose.onNodeWithText("Movies").assertIsDisplayed()
        compose.onNodeWithText("2 movies").assertIsDisplayed()
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.press(Key.DirectionCenter)
        assertEquals("m1", opened)
        compose.press(Key.DirectionRight)
        compose.press(Key.DirectionCenter)
        assertEquals("m2", opened)
    }

    @Test
    fun `an empty movie list says what to do`() {
        show { MoviesScreen(movies = emptyList(), resume = emptyMap(), onWatch = {}) }
        compose.onNodeWithText("Nothing to show. Check Settings.").assertIsDisplayed()
    }

    @Test
    fun `shows are listed with their episode count, and OK opens the show`() {
        val lupin = Show("Lupin", listOf(episode("e1", "Lupin - S01E01 - One"), episode("e2", "Lupin - S01E02 - Two")))
        val solo = Show("Solo", listOf(episode("e3", "Solo - S01E01 - Only")))
        var opened: Show? = null
        show { ShowsScreen(shows = listOf(lupin, solo), onOpen = { opened = it }) }

        compose.onNodeWithText("2 shows").assertIsDisplayed()
        compose.onNodeWithText("2 episodes").assertIsDisplayed()
        compose.onNodeWithText("1 episode").assertIsDisplayed()
        compose.press(Key.DirectionRight)
        compose.press(Key.DirectionCenter)
        assertEquals(solo, opened)
    }

    @Test
    fun `episodes drop the show name from their title, and OK plays one`() {
        val lupin = Show("Lupin", listOf(episode("e1", "Lupin - S01E01 - One"), episode("e2", "Lupin - S01E02 - Two")))
        var played: String? = null
        show { EpisodesScreen(show = lupin, resume = emptyMap(), onWatch = { played = it }) }

        compose.onNodeWithText("Lupin").assertIsDisplayed()
        compose.onNodeWithText("2 episodes").assertIsDisplayed()
        compose.onNodeWithText("S01E01 - One").assertIsDisplayed()
        compose.press(Key.DirectionDown)
        compose.press(Key.DirectionCenter)
        assertEquals("e2", played)
    }
}
