package com.calias.tv.core.epg

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgramTest {

    private val program = Program(startMs = 1_000, stopMs = 5_000, title = "x")

    @Test
    fun `progress is the elapsed share, clamped to 0 and 1`() {
        assertEquals(0f, program.progress(0))
        assertEquals(0.5f, program.progress(3_000))
        assertEquals(1f, program.progress(9_000))
    }
}
