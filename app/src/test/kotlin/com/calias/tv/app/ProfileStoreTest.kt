package com.calias.tv.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import com.calias.tv.core.profile.Profiles
import com.calias.tv.core.profile.Source
import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/** The profile store on a DataStore in a temporary file: no device, no network, no stored settings of the app. */
class ProfileStoreTest {

    private val mum = Source.Direct("https://a.example/mum.m3u")
    private val dad = Source.Direct("https://a.example/dad.m3u")
    private val movie = Channel("m1", "Some Movie (2020)", "http://x/m1", group = null, kind = ContentKind.MOVIE)
    private val live = Channel("l1", "Live One", "http://x/l1", group = null)

    private fun TestScope.newStore(): ProfileStore {
        val file = File.createTempFile("profiles", ".preferences_pb").also { it.delete(); it.deleteOnExit() }
        return ProfileStore(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
    }

    @Test
    fun `a new profile becomes the active one`() = runTest {
        val store = newStore()
        assertNull(store.profiles.first().active)

        store.add("Mum", mum, id = "a")
        store.add("Dad", dad, id = "b")

        val state = store.profiles.first()
        assertEquals(listOf("Mum", "Dad"), state.profiles.map { it.name })
        assertEquals("b", state.active?.id)
    }

    @Test
    fun `each profile has its own look, recents and resume points`() = runTest {
        val store = newStore()
        store.add("Mum", mum, id = "a")
        store.setFont(AppFont.INTER)
        store.setSubtitleSize(SubtitleSize.LARGE)
        store.toggleDark()
        store.saveRecentLive(live)
        store.saveProgress(movie, positionMs = 60_000, durationMs = 600_000, nowMs = 1)

        store.add("Dad", dad, id = "b")
        assertEquals(Look.DEFAULT, store.look.first())
        assertEquals(emptyList(), store.recentLive.first())
        assertEquals(0L, store.startPosition(movie))

        store.switchTo("a")
        assertEquals(Look(dark = false, font = AppFont.INTER, subtitleSize = SubtitleSize.LARGE), store.look.first())
        assertEquals(listOf(live.streamUrl), store.recentLive.first())
        assertEquals(60_000L, store.startPosition(movie))
    }

    @Test
    fun `each profile remembers its own audio and subtitle language, and loses them with the profile`() = runTest {
        val store = newStore()
        assertEquals(TrackPrefs(), store.trackPrefs.first())
        store.add("Mum", mum, id = "a")
        store.setAudioLanguage("pt")
        store.setSubtitleLanguage("en")
        assertEquals(TrackPrefs(audio = "pt", subtitles = "en"), store.trackPrefs.first())

        store.add("Dad", dad, id = "b")
        assertEquals(TrackPrefs(), store.trackPrefs.first())
        store.setSubtitleLanguage(TrackPrefs.OFF)
        assertEquals(TrackPrefs(audio = null, subtitles = TrackPrefs.OFF), store.trackPrefs.first())

        store.switchTo("a")
        assertEquals(TrackPrefs(audio = "pt", subtitles = "en"), store.trackPrefs.first())

        store.remove("a")
        store.add("Again", mum, id = "a")
        assertEquals(TrackPrefs(), store.trackPrefs.first())
    }

    @Test
    fun `editing a profile keeps what it remembers`() = runTest {
        val store = newStore()
        store.add("Mum", mum, id = "a")
        store.saveRecentLive(live)

        store.update("a", "Mother", dad)

        val profile = store.profiles.first().active
        assertEquals("Mother", profile?.name)
        assertEquals(dad, profile?.source)
        assertEquals(listOf(live.streamUrl), store.recentLive.first())
    }

    @Test
    fun `removing a profile erases what it remembered and leaves none active`() = runTest {
        val store = newStore()
        store.add("Mum", mum, id = "a")
        store.setFont(AppFont.INTER)
        store.saveRecentLive(live)

        store.remove("a")

        assertEquals(emptyList(), store.profiles.first().profiles)
        assertNull(store.profiles.first().activeId)
        // The same id again starts clean: nothing of the removed profile is left behind.
        store.add("Again", mum, id = "a")
        assertEquals(Look.DEFAULT, store.look.first())
        assertEquals(emptyList(), store.recentLive.first())
    }

    @Test
    fun `removing another profile keeps the active one`() = runTest {
        val store = newStore()
        store.add("Mum", mum, id = "a")
        store.add("Dad", dad, id = "b")

        store.remove("a")

        assertEquals("b", store.profiles.first().activeId)
    }

    @Test
    fun `switching to an id that is not in the list does nothing`() = runTest {
        val store = newStore()
        store.add("Mum", mum, id = "a")

        store.switchTo("ghost")

        assertEquals("a", store.profiles.first().activeId)
    }

    @Test
    fun `with no active profile nothing is saved and the defaults show`() = runTest {
        val store = newStore()

        store.setFont(AppFont.INTER)
        store.saveRecentLive(live)
        store.saveProgress(movie, 60_000, 600_000, 1)

        assertEquals(Look.DEFAULT, store.look.first())
        assertEquals(emptyList(), store.recentLive.first())
        assertEquals(emptyMap(), store.resume.first())
    }

    @Test
    fun `the list is full at the limit`() = runTest {
        val store = newStore()
        repeat(Profiles.MAX) { store.add("P$it", mum, id = "$it") }

        assertFailsWith<IllegalArgumentException> { store.add("Too many", mum, id = "x") }
        assertEquals(Profiles.MAX, store.profiles.first().profiles.size)
    }
}
