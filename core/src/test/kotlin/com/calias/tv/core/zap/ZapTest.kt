package com.calias.tv.core.zap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZapTest {

    @Test
    fun `steps forward and backward`() {
        assertEquals(3, Zap.step(index = 2, delta = 1, size = 10))
        assertEquals(1, Zap.step(index = 2, delta = -1, size = 10))
    }

    @Test
    fun `wraps around both ends of the list`() {
        assertEquals(0, Zap.step(index = 9, delta = 1, size = 10))
        assertEquals(9, Zap.step(index = 0, delta = -1, size = 10))
    }

    @Test
    fun `handles several steps and a single item list`() {
        assertEquals(2, Zap.step(index = 8, delta = 4, size = 10))
        assertEquals(0, Zap.step(index = 0, delta = -1, size = 1))
    }

    @Test
    fun `rejects an empty list`() {
        assertFailsWith<IllegalArgumentException> { Zap.step(index = 0, delta = 1, size = 0) }
    }
}
