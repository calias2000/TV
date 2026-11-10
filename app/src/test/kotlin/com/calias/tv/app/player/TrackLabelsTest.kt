package com.calias.tv.app.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** How the audio and subtitle tracks of a file are named in the menu. */
@RunWith(RobolectricTestRunner::class)
class TrackLabelsTest {

    private fun audio(language: String?, channels: Int, supported: Boolean = true, selected: Boolean = false) = group(
        Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AC3).setLanguage(language).setChannelCount(channels).build(),
        supported,
        selected,
    )

    private fun text(language: String?, label: String? = null) = group(
        Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_SUBRIP).setLanguage(language).setLabel(label).build(),
        supported = true,
        selected = false,
    )

    private fun group(format: Format, supported: Boolean, selected: Boolean) = Tracks.Group(
        TrackGroup(format),
        /* adaptiveSupported = */ false,
        intArrayOf(if (supported) C.FORMAT_HANDLED else C.FORMAT_UNSUPPORTED_TYPE),
        booleanArrayOf(selected),
    )

    @Test
    fun `audio is named by language, with surround marked`() {
        val tracks = Tracks(listOf(audio("fr", 6), audio("en", 2), audio("ja", 8)))
        assertEquals(listOf("French · 5.1", "English", "Japanese · 7.1"), options(tracks, C.TRACK_TYPE_AUDIO).map { it.label })
    }

    @Test
    fun `a language used twice is numbered`() {
        val tracks = Tracks(listOf(audio("es", 2), audio("es", 6), audio("es", 2)))
        assertEquals(
            listOf("Spanish", "Spanish 2 · 5.1", "Spanish 3"),
            options(tracks, C.TRACK_TYPE_AUDIO).map { it.label },
        )
    }

    @Test
    fun `a track without a known language uses its label, or Unknown`() {
        val tracks = Tracks(listOf(text(null, "Commentary"), text(C.LANGUAGE_UNDETERMINED), text("pt")))
        assertEquals(listOf("Commentary", "Unknown", "Portuguese"), options(tracks, C.TRACK_TYPE_TEXT).map { it.label })
    }

    @Test
    fun `audio and subtitles are kept apart`() {
        val tracks = Tracks(listOf(audio("fr", 2), text("en")))
        assertEquals(listOf("French"), options(tracks, C.TRACK_TYPE_AUDIO).map { it.label })
        assertEquals(listOf("English"), options(tracks, C.TRACK_TYPE_TEXT).map { it.label })
    }

    @Test
    fun `what the TV cannot play is marked, and so is what plays now`() {
        val tracks = Tracks(listOf(audio("fr", 6, supported = true, selected = true), audio("de", 6, supported = false)))
        val (french, german) = options(tracks, C.TRACK_TYPE_AUDIO)
        assertTrue(french.supported && french.selected)
        assertFalse(german.supported || german.selected)
    }
}
