package com.calias.tv.core.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QualityTest {

    @Test
    fun `common sizes get their common name`() {
        assertEquals("4K", Quality.label(3840, 2160))
        assertEquals("QHD", Quality.label(2560, 1440))
        assertEquals("Full HD", Quality.label(1920, 1080))
        assertEquals("HD", Quality.label(1280, 720))
        assertEquals("SD", Quality.label(1024, 576))
        assertEquals("SD", Quality.label(720, 576))
    }

    @Test
    fun `a wide film frame keeps the name of its width`() {
        assertEquals("Full HD", Quality.label(1920, 800))
        assertEquals("HD", Quality.label(1280, 544))
        assertEquals("4K", Quality.label(3840, 1600))
    }

    @Test
    fun `an unknown size has no name`() {
        assertNull(Quality.label(0, 0))
    }
}
