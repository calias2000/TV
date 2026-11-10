package com.calias.tv.app

import com.calias.tv.core.Channel
import com.calias.tv.core.playlist.Playlist
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test

class HelpersTest {

    private val zone = TimeZone.getDefault()

    @Before
    fun fixZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @After
    fun restoreZone() = TimeZone.setDefault(zone)

    @Test
    fun `times are shown on a 24 hour clock, never AM or PM`() {
        assertEquals("00:05", formatTime(5 * 60_000L))
        assertEquals("21:30", formatTime((21 * 60 + 30) * 60_000L))
        assertEquals("13:00", formatTime(13 * 3_600_000L))
    }

    @Test
    fun `counts say one in the singular and the rest in the plural`() {
        assertEquals("1 movie", countOf(1, "movie"))
        assertEquals("2 movies", countOf(2, "movie"))
        assertEquals("0 episodes", countOf(0, "episode"))
    }

    @Test
    fun `the playlist status says what happened, and nothing while loading`() {
        assertNull(statusText(PlaylistState.Loading))
        val playlist = Playlist(null, listOf(Channel("1", "One", "http://x/1", null)))
        assertEquals("Playlist loaded: 1 entries", statusText(PlaylistState.Loaded(playlist)))
        assertEquals(
            "Playlist unavailable: no network. Press Sync to retry.",
            statusText(PlaylistState.Failed("no network")),
        )
    }
}
