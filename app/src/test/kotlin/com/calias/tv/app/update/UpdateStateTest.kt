package com.calias.tv.app.update

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class UpdateStateTest {

    private val release = Release("0.2.0", "http://x/app.apk", 10, null)

    @Test
    fun `Settings says where the update is and offers what can be done`() {
        assertNull(updateText(UpdateState.Idle))
        assertEquals("You have the latest version.", updateText(UpdateState.UpToDate))
        assertEquals("Version 0.2.0 is available.", updateText(UpdateState.Available(release)))
        assertEquals("Downloading version 0.2.0: 40%", updateText(UpdateState.Downloading(release, 40)))
        assertEquals("Update failed: no network", updateText(UpdateState.Failed("no network")))

        assertEquals("Check for updates", updateButton(UpdateState.UpToDate))
        assertEquals("Check for updates", updateButton(UpdateState.Failed("no network")))
        assertEquals("Install version 0.2.0", updateButton(UpdateState.Available(release)))
        assertEquals("Try again", updateButton(UpdateState.Failed("refused", release)))
        assertNull(updateButton(UpdateState.Checking), "nothing to press while it checks")
        assertNull(updateButton(UpdateState.Downloading(release, 1)))
        assertNull(updateButton(UpdateState.Installing(release)))
    }

    @Test
    fun `Home has a button only for an update that was found`() {
        assertEquals("Update to 0.2.0", homeUpdateButton(UpdateState.Available(release)))
        assertEquals("Downloading 40%", homeUpdateButton(UpdateState.Downloading(release, 40)))
        assertEquals("Installing…", homeUpdateButton(UpdateState.Installing(release)))
        assertEquals("Retry update", homeUpdateButton(UpdateState.Failed("refused", release)))
        listOf(UpdateState.Idle, UpdateState.Checking, UpdateState.UpToDate, UpdateState.Failed("no network")).forEach {
            assertNull(homeUpdateButton(it), "$it")
        }
    }

    @Test
    fun `only a found update carries its release`() {
        assertNull(UpdateState.Failed("no network").release)
        assertEquals(release, UpdateState.Failed("refused", release).release)
        assertEquals(release, UpdateState.Available(release).release)
    }
}
