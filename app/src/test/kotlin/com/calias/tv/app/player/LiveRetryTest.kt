package com.calias.tv.app.player

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveRetryTest {

    @Test
    fun `each drop waits longer, 1 then 2 then 4 seconds`() = runTest {
        val retry = LiveRetry()
        val waited = mutableListOf<Long>()
        repeat(3) {
            val start = currentTime
            retry.onDrop { waited += currentTime - start }
        }
        assertEquals(listOf(1_000L, 2_000L, 4_000L), waited)
        assertFalse(retry.exhausted)
    }

    @Test
    fun `after the last wait the next drop is not retried and turns exhausted on`() = runTest {
        val retry = LiveRetry()
        repeat(3) { retry.onDrop {} }
        var retried = false
        retry.onDrop { retried = true }
        assertFalse(retried)
        assertTrue(retry.exhausted)
    }

    @Test
    fun `reset starts again from the first wait`() = runTest {
        val retry = LiveRetry()
        repeat(3) { retry.onDrop {} }
        retry.onDrop {}
        retry.reset()
        assertFalse(retry.exhausted)

        val start = currentTime
        var waited = 0L
        retry.onDrop { waited = currentTime - start }
        assertEquals(1_000L, waited)
    }

    @Test
    fun `a wait that is cancelled costs nothing and does not retry`() = runTest {
        val retry = LiveRetry()
        var retried = false
        val job = launch { retry.onDrop { retried = true } }
        runCurrent()
        advanceTimeBy(500) // part of the first second
        job.cancel() // the user zapped away
        runCurrent()
        assertFalse(retried)

        val start = currentTime
        var waited = 0L
        retry.onDrop { waited = currentTime - start }
        assertEquals(1_000L, waited) // still the first wait
    }

    @Test
    fun `the waits can be chosen`() = runTest {
        val retry = LiveRetry(waits = listOf(3.seconds))
        retry.onDrop {}
        retry.onDrop {}
        assertTrue(retry.exhausted)
        assertEquals(3_000L, currentTime)
    }
}
