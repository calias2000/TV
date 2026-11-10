package com.calias.tv.app.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.core.Channel
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.epg.Program
import com.calias.tv.core.library.Category
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The browse panel over the video, with made-up channels and guide. Time is driven by hand
 * (`mainClock`), so the half-second waits of the panel take no real time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ChannelPanelTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = 1_000_000_000L
    private fun channel(name: String) = Channel(name, name, "http://x/$name", group = null, tvgId = "$name.id")

    private val groups = listOf(
        Category("News", listOf(channel("a"), channel("b"), channel("c"))),
        Category("Sport", listOf(channel("d"))),
    )

    // Every channel has one programme running now, named after it: "Show a", "Show b", ...
    private val guide = Guide(
        listOf("a", "b", "c", "d").associate { id ->
            "$id.id" to listOf(Program(now - 3_600_000, now + 3_600_000, "Show $id"))
        },
    )

    private val state = PanelState(startOpen = true, group = 0)
    private var picked: Pair<Int, Int>? = null

    private fun panel(playingIndex: Int = 0) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) {
                ChannelPanel(
                    state = state,
                    groups = groups,
                    playingGroup = 0,
                    playingIndex = playingIndex,
                    started = true,
                    guide = guide,
                    nowMs = now,
                    showGuide = true,
                    onPick = { group, index -> picked = group to index },
                )
            }
        }
        advance(600) // opening animation, first focus
    }

    private fun advance(ms: Long) = compose.mainClock.advanceTimeBy(ms)

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `it lists the channels of the group and the playing one has focus`() {
        panel(playingIndex = 1)
        compose.onNodeWithText("News").assertIsDisplayed()
        compose.onNodeWithText("a").assertIsDisplayed()
        press(Key.DirectionCenter)
        assertEquals(0 to 1, picked)
    }

    @Test
    fun `DOWN then OK picks the next channel`() {
        panel()
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals(0 to 1, picked)
    }

    @Test
    fun `LEFT from the channels goes up to the groups`() {
        panel()
        press(Key.DirectionLeft)
        assertEquals(PanelLevel.GROUPS, state.level)
    }

    @Test
    fun `the guide card shows the channel under the cursor once it rests`() {
        panel()
        compose.onNodeWithText("Show a").assertIsDisplayed()

        press(Key.DirectionDown)
        advance(50) // the cursor has moved, but not rested yet
        compose.onNodeWithText("Show a").assertIsDisplayed()

        advance(400)
        compose.onNodeWithText("Show b").assertIsDisplayed()
    }

    @Test
    fun `RIGHT right after a move first brings the guide up to date, and does not enter the old guide`() {
        panel()
        press(Key.DirectionDown)
        press(Key.DirectionRight) // before the guide has caught up
        advance(100)
        compose.onNodeWithText("Show b").assertIsDisplayed()

        // Focus is still on the channel list: OK picks the channel under the cursor.
        press(Key.DirectionCenter)
        assertEquals(0 to 1, picked)
    }

    @Test
    fun `a closed panel shows nothing`() {
        state.close()
        panel()
        advance(600)
        assertNull(picked)
        assertEquals(false, state.visible)
    }
}
