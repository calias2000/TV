package com.calias.tv.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.calias.tv.app.ui.AppFont
import androidx.tv.material3.Text
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.ui.GlassDialog
import com.calias.tv.app.ui.GlassSideSheet
import com.calias.tv.core.profile.Profile
import com.calias.tv.core.profile.Profiles
import com.calias.tv.core.profile.Source
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The profile list, the profile form, the modal around them and the no-profile screen, with made-up profiles: no network, no stored settings. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ProfileDialogsTest {

    @get:Rule
    val compose = createComposeRule()

    private val mum = Profile("a", "Mum", Source.Direct("https://tv.example/live/secretuser/secretpass/list.m3u"))
    private val dad = Profile("b", "Dad", Source.Direct("https://other.example/dad.m3u"))

    @Test
    fun `the list shows each name, marks the active one, and shows nothing of the address`() {
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) { ProfileList(ProfilesState(listOf(mum, dad), "b"), onPick = {}) }
        }
        compose.onNodeWithText("Mum").assertIsDisplayed()
        compose.onNodeWithText("Dad").assertIsDisplayed()
        compose.onNodeWithText("✓").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("tv.example", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("secretpass", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `picking a row reports that profile`() {
        var picked: Profile? = null
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) { ProfileList(ProfilesState(listOf(mum, dad), "a"), onPick = { picked = it }) }
        }
        compose.onNodeWithText("Dad").focusAndPressOk(compose)
        assertEquals(dad, picked)
    }

    @Test
    fun `a full set of profiles fits the side sheet with its footer`() {
        val full = (1..Profiles.MAX).map { Profile("$it", "Person $it", mum.source) }
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) {
                GlassSideSheet(title = "Profiles", onDismiss = {}, footer = { Text("Footer") }) { ProfileList(ProfilesState(full, "1"), onPick = {}) }
            }
        }
        compose.onNodeWithText("Profiles").assertIsDisplayed()
        compose.onNodeWithText("Person ${Profiles.MAX}").assertIsDisplayed()
        compose.onNodeWithText("Footer").assertIsDisplayed()
    }

    private var saved: Pair<String, Source>? = null
    private var done = 0
    private var deleted = 0

    private fun form(existing: Profile?, failure: String? = null, serverBase: String = "http://pc:5002") = compose.setContent {
        AppTheme(dark = true, font = AppFont.SYSTEM) {
            ProfileForm(
                existing = existing,
                defaultName = "Profile 1",
                serverBase = serverBase,
                onSave = { name, source -> saved = name to source; failure },
                onDone = { done++ },
            )
        }
    }

    private fun fields() = compose.onAllNodes(hasSetTextAction())

    @Test
    fun `a new profile starts with the default name and saves a cleaned name and the address`() {
        form(existing = null)
        compose.onNodeWithText("Profile 1").assertIsDisplayed()

        fields()[0].performTextClearance()
        fields()[0].performTextInput("  Mum  ")
        fields()[1].performTextInput(" https://a.example/mum.m3u ")
        compose.onNodeWithText("Save").focusAndPressOk(compose)
        compose.waitForIdle()

        assertEquals("Mum" to Source.Direct("https://a.example/mum.m3u"), saved)
        assertEquals(1, done)
    }

    @Test
    fun `a code is saved as the playlist link of that code on the server`() {
        form(existing = null)
        fields()[1].performTextInput(" ab-23 ")
        compose.onNodeWithText("Save").focusAndPressOk(compose)
        compose.waitForIdle()

        assertEquals("Profile 1" to Source.Direct("http://pc:5002/api/iptv/p/AB23/list"), saved)
    }

    @Test
    fun `a code in a build without a server address is explained`() {
        form(existing = null, serverBase = "")
        fields()[1].performTextInput("AB23")
        compose.onNodeWithText("Save").focusAndPressOk(compose)

        compose.onNodeWithText("This app was built without a server address, so codes do not work. Type the full address.").assertIsDisplayed()
        assertNull(saved)
    }

    @Test
    fun `a missing name or a bad address is explained and nothing is saved`() {
        form(existing = null)

        compose.onNodeWithText("Save").focusAndPressOk(compose)
        compose.onNodeWithText("Enter the code you were given, or an address that starts with http:// or https:// and has no spaces.").assertIsDisplayed()

        fields()[0].performTextClearance()
        fields()[1].performTextInput("https://a.example/x.m3u")
        compose.onNodeWithText("Save").focusAndPressOk(compose)
        compose.onNodeWithText("Give the profile a name.").assertIsDisplayed()

        assertNull(saved)
        assertEquals(0, done)
    }

    @Test
    fun `when the playlist cannot be loaded the reason shows and the form stays`() {
        form(existing = null, failure = "Playlist server answered HTTP 404")
        fields()[1].performTextInput("https://a.example/x.m3u")

        compose.onNodeWithText("Save").focusAndPressOk(compose)
        compose.waitForIdle()

        compose.onNodeWithText("Playlist server answered HTTP 404").assertIsDisplayed()
        assertEquals(0, done)
    }

    @Test
    fun `editing shows the profile being edited`() {
        form(existing = mum)
        compose.onNodeWithText("Mum").assertIsDisplayed()
    }

    @Test
    fun `Delete needs a second press`() {
        compose.setContent { AppTheme(dark = true, font = AppFont.SYSTEM) { DeleteProfileButton(onDelete = { deleted++ }) } }

        compose.onNodeWithText("Delete profile").focusAndPressOk(compose)
        assertEquals(0, deleted)
        compose.onNodeWithText("Press again to delete").focusAndPressOk(compose)
        assertEquals(1, deleted)
    }

    @Test
    fun `the modal shows its title, its action and its content`() {
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) {
                GlassDialog(title = "Profiles", onDismiss = {}, action = { Text("Action") }) { Text("Inside") }
            }
        }
        compose.onNodeWithText("Profiles").assertIsDisplayed()
        compose.onNodeWithText("Action").assertIsDisplayed()
        compose.onNodeWithText("Inside").assertIsDisplayed()
    }

    @Test
    fun `with no profile the one tile adds a profile`() {
        var opens = 0
        compose.setContent { AppTheme(dark = true, font = AppFont.SYSTEM) { NoProfileScreen(hasProfiles = false, onOpen = { opens++ }) } }
        compose.onNodeWithText("Add a playlist to start watching").assertIsDisplayed()
        compose.onNodeWithText("Add profile").focusAndPressOk(compose)
        assertEquals(1, opens)
    }

    @Test
    fun `with profiles but none chosen the one tile opens the list`() {
        compose.setContent { AppTheme(dark = true, font = AppFont.SYSTEM) { NoProfileScreen(hasProfiles = true, onOpen = {}) } }
        compose.onNodeWithText("Choose profile").assertIsDisplayed()
    }
}
