package com.calias.tv.app.player

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PanelStateTest {

    @Test
    fun `live starts open, movies start closed`() {
        assertTrue(PanelState(startOpen = true, group = 0).visible)
        assertFalse(PanelState(startOpen = false, group = 0).visible)
    }

    @Test
    fun `open shows the channels of the playing group`() {
        val panel = PanelState(startOpen = false, group = 0)
        panel.showGroups()
        panel.open(playingGroup = 3)
        assertTrue(panel.visible)
        assertEquals(3, panel.group)
        assertEquals(PanelLevel.CHANNELS, panel.level)
    }

    @Test
    fun `groups and channels levels switch, and a group can be chosen`() {
        val panel = PanelState(startOpen = true, group = 0)
        panel.showGroups()
        assertEquals(PanelLevel.GROUPS, panel.level)
        panel.showChannels(2)
        assertEquals(PanelLevel.CHANNELS, panel.level)
        assertEquals(2, panel.group)
    }

    @Test
    fun `close hides it`() {
        val panel = PanelState(startOpen = true, group = 0)
        panel.close()
        assertFalse(panel.visible)
    }
}
