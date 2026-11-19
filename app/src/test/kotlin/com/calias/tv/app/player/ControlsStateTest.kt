package com.calias.tv.app.player

import androidx.compose.ui.input.key.Key
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ControlsStateTest {

    @Test
    fun `the bar keys do nothing until DOWN highlights the first button`() {
        val controls = ControlsState()
        assertFalse(controls.onBarKey(Key.DirectionLeft))
        assertFalse(controls.onBarKey(Key.DirectionCenter))
        assertNull(controls.button)

        assertTrue(controls.onBarKey(Key.DirectionDown))
        assertEquals(TrackMenu.AUDIO, controls.button)
    }

    @Test
    fun `LEFT and RIGHT move between the buttons and stop at the ends`() {
        val controls = ControlsState().apply { onBarKey(Key.DirectionDown) }
        controls.onBarKey(Key.DirectionRight)
        assertEquals(TrackMenu.SUBTITLES, controls.button)
        controls.onBarKey(Key.DirectionRight)
        assertEquals(TrackMenu.SUBTITLES, controls.button)
        controls.onBarKey(Key.DirectionLeft)
        assertEquals(TrackMenu.AUDIO, controls.button)
        controls.onBarKey(Key.DirectionLeft)
        assertEquals(TrackMenu.AUDIO, controls.button)
    }

    @Test
    fun `OK opens the card, BACK-style close keeps the button, hide clears everything`() {
        val controls = ControlsState().apply { onBarKey(Key.DirectionDown) }
        assertTrue(controls.onBarKey(Key.DirectionCenter))
        assertFalse(controls.menuOpen) // opens when OK is let go, not when it goes down
        assertTrue(controls.onBarKeyUp(Key.DirectionCenter))
        assertTrue(controls.menuOpen)

        controls.closeMenu()
        assertFalse(controls.menuOpen)
        assertEquals(TrackMenu.AUDIO, controls.button)

        controls.onBarKey(Key.DirectionCenter)
        controls.onBarKeyUp(Key.DirectionCenter)
        controls.hide()
        assertFalse(controls.shown)
        assertFalse(controls.menuOpen)
        assertNull(controls.button)
    }

    @Test
    fun `a release opens the card only for OK, with a button highlighted and the card closed`() {
        val controls = ControlsState()
        assertFalse(controls.onBarKeyUp(Key.DirectionCenter)) // no button highlighted

        controls.onBarKey(Key.DirectionDown)
        assertFalse(controls.onBarKeyUp(Key.DirectionLeft))
        assertFalse(controls.menuOpen)

        assertTrue(controls.onBarKeyUp(Key.Enter))
        assertTrue(controls.menuOpen)
        assertFalse(controls.onBarKeyUp(Key.DirectionCenter)) // already open: the release belongs to the card's row
    }

    @Test
    fun `UP leaves the buttons and DOWN on a button is used up`() {
        val controls = ControlsState().apply { onBarKey(Key.DirectionDown) }
        assertTrue(controls.onBarKey(Key.DirectionDown))
        assertEquals(TrackMenu.AUDIO, controls.button)
        assertTrue(controls.onBarKey(Key.DirectionUp))
        assertNull(controls.button)
    }

    @Test
    fun `skips in a row add up, and a skip the other way starts again`() {
        val controls = ControlsState()
        controls.flashSkip(10_000)
        controls.flashSkip(10_000)
        assertEquals("+20 s", controls.skip.label)
        assertTrue(controls.skip.forward)

        controls.flashSkip(-10_000)
        assertEquals("-10 s", controls.skip.label)
        assertFalse(controls.skip.forward)
    }

    @Test
    fun `two identical skips count as two events`() {
        val controls = ControlsState()
        controls.flashSkip(10_000)
        val first = controls.skip.id
        controls.flashSkip(10_000)
        assertTrue(controls.skip.id > first)
    }
}
