package com.calias.tv.app.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.calias.tv.app.TrackPrefs
import com.calias.tv.app.press
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.ui.SubtitleSize
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** What picking a track remembers and applies, and the text size row of the subtitles card. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class TrackChoiceTest {

    @get:Rule
    val compose = createComposeRule()

    private val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
    private val choices = mutableListOf<Pair<TrackMenu, String>>()
    private val tracks = TracksState(player) { menu, value -> choices += menu to value }

    private fun group(mime: String, language: String?, selected: Boolean = false) = Tracks.Group(
        TrackGroup(Format.Builder().setSampleMimeType(mime).setLanguage(language).build()),
        false,
        intArrayOf(C.FORMAT_HANDLED),
        booleanArrayOf(selected),
    )

    private fun offered() = tracks.update(
        Tracks(
            listOf(
                group(MimeTypes.AUDIO_AC3, "en", selected = true),
                group(MimeTypes.AUDIO_AC3, "pt"),
                group(MimeTypes.APPLICATION_SUBRIP, "en"),
                group(MimeTypes.APPLICATION_SUBRIP, null),
            ),
        ),
    )

    @Test
    fun `picking an audio track remembers its language and prefers it for the next movie`() {
        offered()
        tracks.select(tracks.audio[1])

        assertEquals(listOf(TrackMenu.AUDIO to "pt"), choices)
        assertEquals(listOf("pt"), player.trackSelectionParameters.preferredAudioLanguages)
    }

    @Test
    fun `picking a subtitle track turns subtitles on in that language, Off is remembered as off`() {
        offered()
        tracks.hideSubtitles()
        assertEquals(TrackMenu.SUBTITLES to TrackPrefs.OFF, choices.last())
        assertTrue(C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes)

        tracks.select(tracks.text[0])
        assertEquals(TrackMenu.SUBTITLES to "en", choices.last())
        assertTrue(C.TRACK_TYPE_TEXT !in player.trackSelectionParameters.disabledTrackTypes)
        assertEquals(listOf("en"), player.trackSelectionParameters.preferredTextLanguages)
    }

    @Test
    fun `a track that names no language is played but not remembered`() {
        offered()
        tracks.select(tracks.text[1])
        assertEquals(emptyList(), choices)
    }

    @Test
    fun `the remembered choice is applied, and what was never chosen is left alone`() {
        val base = player.trackSelectionParameters

        val none = base.withPrefs(TrackPrefs())
        assertEquals(base.preferredAudioLanguages, none.preferredAudioLanguages)
        assertEquals(base.disabledTrackTypes, none.disabledTrackTypes)

        val off = base.withPrefs(TrackPrefs(audio = "pt", subtitles = TrackPrefs.OFF))
        assertEquals(listOf("pt"), off.preferredAudioLanguages)
        assertTrue(C.TRACK_TYPE_TEXT in off.disabledTrackTypes)

        val on = off.withPrefs(TrackPrefs(subtitles = "en"))
        assertEquals(listOf("en"), on.preferredTextLanguages)
        assertTrue(C.TRACK_TYPE_TEXT !in on.disabledTrackTypes)
        assertEquals(listOf("pt"), on.preferredAudioLanguages)
    }

    private fun card(menu: TrackMenu, size: SubtitleSize = SubtitleSize.MEDIUM, onSize: (SubtitleSize) -> Unit = {}, onPicked: () -> Unit = {}) {
        offered()
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) {
                TrackMenuCard(visible = true, menu = menu, tracks = tracks, subtitleSize = size, onSubtitleSize = onSize, onPicked = onPicked)
            }
        }
    }

    @Test
    fun `the subtitles card has the text size and a sample, the audio card does not`() {
        card(TrackMenu.SUBTITLES, SubtitleSize.LARGE)
        compose.onNodeWithText("Text size").assertIsDisplayed()
        compose.onNodeWithText("Large").assertIsDisplayed()
        compose.onNodeWithText("This is how subtitles look.").assertIsDisplayed()
    }

    @Test
    fun `the audio card has no text size`() {
        card(TrackMenu.AUDIO)
        assertTrue(compose.onAllNodesWithText("Text size").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `LEFT and RIGHT on the size row change the size and leave the card open`() {
        var size = SubtitleSize.MEDIUM
        var closed = 0
        card(TrackMenu.SUBTITLES, size, onSize = { size = it }, onPicked = { closed++ })

        compose.onNodeWithText("Text size").requestFocus()
        compose.press(Key.DirectionRight)
        assertEquals(SubtitleSize.LARGE, size)
        compose.press(Key.DirectionLeft)
        assertEquals(SubtitleSize.SMALL, size) // this test card still shows MEDIUM, so one step left is SMALL
        assertEquals(0, closed)
    }

    @Test
    fun `OK on the size row goes to the next size and wraps around`() {
        var size: SubtitleSize? = null
        card(TrackMenu.SUBTITLES, SubtitleSize.EXTRA_LARGE, onSize = { size = it })

        compose.onNodeWithText("Text size").requestFocus()
        compose.press(Key.DirectionCenter)
        assertEquals(SubtitleSize.SMALL, size)
    }

    @Test
    fun `picking a track still closes the card`() {
        var closed = 0
        card(TrackMenu.SUBTITLES, onPicked = { closed++ })

        compose.onNodeWithText("English").requestFocus()
        compose.press(Key.DirectionCenter)
        assertEquals(1, closed)
    }
}
