package com.calias.tv.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.update.Release
import com.calias.tv.app.update.UpdateState
import com.calias.tv.core.Channel
import com.calias.tv.core.playlist.Playlist
import com.calias.tv.core.profile.Profile
import com.calias.tv.core.profile.Source
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Settings screen, drawn with a made-up playlist state: no network, no stored settings. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val loaded = PlaylistState.Loaded(
        Playlist(epgUrl = null, channels = listOf(Channel("1", "One", "http://x/1", group = null), Channel("2", "Two", "http://x/2", group = null))),
    )

    private val profile = Profile("a", "Mum", Source.Direct("https://tv.example/live/user/secret/list.m3u"))

    private fun screen(
        state: PlaylistState = loaded,
        onSync: () -> Unit = {},
        onEditProfile: () -> Unit = {},
        font: AppFont = AppFont.SYSTEM,
        onFont: (AppFont) -> Unit = {},
        update: UpdateState = UpdateState.Idle,
        onUpdate: () -> Unit = {},
    ) = compose.setContent {
        AppTheme(dark = true, font = AppFont.SYSTEM) {
            SettingsScreen(profile, state, onSync, onEditProfile, font, onFont, update, onUpdate)
        }
    }

    @Test
    fun `shows the profile name, never its server, and how many entries loaded`() {
        screen()
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Profile: Mum").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("tv.example", substring = true).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Playlist loaded: 2 entries").assertIsDisplayed()
    }

    @Test
    fun `a failed playlist shows the reason and how to retry`() {
        screen(state = PlaylistState.Failed("server did not answer in time"))
        compose.onNodeWithText("Playlist unavailable: server did not answer in time. Press Sync to retry.").assertIsDisplayed()
    }

    @Test
    fun `Sync now asks for a new sync`() {
        var syncs = 0
        screen(onSync = { syncs++ })
        compose.onNodeWithText("Sync now").focusAndPressOk(compose)
        assertEquals(1, syncs)
    }

    @Test
    fun `Edit profile opens the profile form`() {
        var edits = 0
        screen(onEditProfile = { edits++ })
        compose.onNodeWithText("Edit profile").focusAndPressOk(compose)
        assertEquals(1, edits)
    }

    @Test
    fun `the app version is shown, with a button to look for an update`() {
        var asked = 0
        screen(onUpdate = { asked++ })
        compose.onNodeWithText("App version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        compose.onNodeWithText("Check for updates").focusAndPressOk(compose)
        assertEquals(1, asked)
    }

    @Test
    fun `a found update is offered, and while it downloads there is nothing to press`() {
        val release = Release("0.2.0", "http://x/app.apk", 10, null)
        screen(update = UpdateState.Available(release))
        compose.onNodeWithText("Version 0.2.0 is available.").assertIsDisplayed()
        compose.onNodeWithText("Install version 0.2.0").assertIsDisplayed()
    }

    @Test
    fun `the current font has a check mark, and picking one reports it`() {
        var font: AppFont? = null
        screen(font = AppFont.INTER, onFont = { font = it })

        compose.onNodeWithText("✓ Inter").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Manrope").performScrollTo().focusAndPressOk(compose)
        assertEquals(AppFont.MANROPE, font)
    }

}
