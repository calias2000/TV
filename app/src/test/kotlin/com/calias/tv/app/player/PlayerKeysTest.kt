package com.calias.tv.app.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.press
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The player's keys with a stand-in for the card: a focusable row that takes focus when the card opens. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class PlayerKeysTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `OK opens the card when it is let go, and that release does not pick the first row`() {
        val controls = ControlsState().apply { onBarKey(Key.DirectionDown) } // Audio is highlighted
        var picks = 0
        compose.setContent {
            AppTheme(dark = true, font = AppFont.SYSTEM) {
                val screenFocus = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(screenFocus)
                        .focusable()
                        .playerKeys(
                            isLive = false,
                            controlsEnabled = true,
                            listOpen = controls.menuOpen,
                            barOpen = !controls.menuOpen,
                            nextEpisodeOffered = false,
                            onZap = {},
                            onTogglePlay = {},
                            onSkip = { _, _ -> },
                            onShowControls = {},
                            onOpenPanel = {},
                            onNextEpisode = {},
                            onBarKey = controls::onBarKey,
                            onBarKeyUp = controls::onBarKeyUp,
                        ),
                ) {
                    if (controls.menuOpen) {
                        val rowFocus = remember { FocusRequester() }
                        ChannelRow(title = "First row", modifier = Modifier.focusRequester(rowFocus), onClick = { picks++ })
                        LaunchedEffect(Unit) { rowFocus.requestFocus() }
                    }
                }
                LaunchedEffect(Unit) { screenFocus.requestFocus() }
            }
        }

        // A real press: the key goes down and waits there; the card opens only when it comes up.
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.waitForIdle()
        assertFalse(controls.menuOpen)
        compose.onRoot().performKeyInput { keyUp(Key.DirectionCenter) }
        compose.waitForIdle()
        assertTrue(controls.menuOpen)
        assertEquals(0, picks)

        compose.press(Key.DirectionCenter) // a second, deliberate press picks the row
        assertEquals(1, picks)
    }
}
