package com.calias.tv.app.player

import com.calias.tv.core.Channel
import com.calias.tv.core.library.Category
import org.junit.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ZapStateTest {

    private fun channel(name: String) = Channel(id = name, name = name, streamUrl = "http://x/$name", group = null)

    private val groups = listOf(
        Category("News", listOf(channel("a"), channel("b"), channel("c"))),
        Category("Sport", listOf(channel("d"), channel("e"))),
    )

    @Test
    fun `zapping changes the shown channel at once and wraps around`() {
        val zap = ZapState(groups, initialGroup = 0, initialIndex = 2)
        zap.zap(+1)
        assertEquals(0, zap.pending)
        zap.zap(-1)
        assertEquals(2, zap.pending)
        assertEquals(2, zap.loaded)
    }

    @Test
    fun `select jumps to a group and channel and loads it at once`() {
        val zap = ZapState(groups, initialGroup = 0, initialIndex = 0)
        zap.select(group = 1, index = 1)
        assertEquals(1, zap.group)
        assertEquals(1, zap.loaded)
        assertEquals(1, zap.pending)
        assertEquals("e", zap.channels[zap.loaded].name)
    }

    @Test
    fun `settle loads the pending channel only after the keys stop`() = runTest {
        val zap = ZapState(groups, initialGroup = 0, initialIndex = 0)
        var savedWhileLoaded = -1
        zap.zap(+1)
        zap.zap(+1)
        launch { zap.settle(beforeLoad = { savedWhileLoaded = zap.loaded }) }
        runCurrent()
        assertEquals(0, zap.loaded) // still waiting
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, zap.loaded)
        assertEquals(0, savedWhileLoaded) // the old channel was still loaded when beforeLoad ran
    }

    @Test
    fun `settle does nothing when nothing is pending`() = runTest {
        val zap = ZapState(groups, initialGroup = 0, initialIndex = 1)
        var called = false
        zap.settle(beforeLoad = { called = true })
        assertEquals(1, zap.loaded)
        assertEquals(false, called)
    }
}
