package com.calias.tv.core.seek

import kotlin.test.Test
import kotlin.test.assertEquals

class SeekTest {

    @Test
    fun `step grows while the key is held`() {
        assertEquals(10_000L, Seek.stepMs(0))
        assertEquals(10_000L, Seek.stepMs(4))
        assertEquals(30_000L, Seek.stepMs(5))
        assertEquals(30_000L, Seek.stepMs(14))
        assertEquals(60_000L, Seek.stepMs(15))
    }

    @Test
    fun `target stays between zero and the duration`() {
        assertEquals(70_000L, Seek.target(positionMs = 60_000, deltaMs = 10_000, durationMs = 600_000))
        assertEquals(0L, Seek.target(positionMs = 5_000, deltaMs = -10_000, durationMs = 600_000))
        assertEquals(600_000L, Seek.target(positionMs = 595_000, deltaMs = 60_000, durationMs = 600_000))
    }

    @Test
    fun `unknown duration only limits the lower bound`() {
        assertEquals(70_000L, Seek.target(positionMs = 60_000, deltaMs = 10_000, durationMs = 0))
        assertEquals(0L, Seek.target(positionMs = 1_000, deltaMs = -10_000, durationMs = -1))
    }

    @Test
    fun `clock shows minutes, then hours, and never goes negative`() {
        assertEquals("0:00", Seek.formatClock(0))
        assertEquals("1:05", Seek.formatClock(65_000))
        assertEquals("59:59", Seek.formatClock(3_599_000))
        assertEquals("1:00:00", Seek.formatClock(3_600_000))
        assertEquals("1:45:07", Seek.formatClock(6_307_000))
        assertEquals("0:00", Seek.formatClock(-5_000))
    }
}
