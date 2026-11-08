package com.calias.tv.core.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DominantTest {

    private val red = 0xFFE02020.toInt()
    private val blue = 0xFF2040E0.toInt()
    private val gray = 0xFF808080.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun `a single clear color is returned as it is`() {
        assertEquals(red, Dominant.color(intArrayOf(red, red, red)))
    }

    @Test
    fun `grays, black and transparent pixels are ignored`() {
        val transparentRed = 0x10E02020
        assertEquals(blue, Dominant.color(intArrayOf(gray, black, transparentRed, blue)))
    }

    @Test
    fun `a picture without a clear color gives null`() {
        assertNull(Dominant.color(intArrayOf(gray, black, 0xFFFFFFFF.toInt())))
        assertNull(Dominant.color(intArrayOf()))
    }
}
