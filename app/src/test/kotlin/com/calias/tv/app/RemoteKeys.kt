package com.calias.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus

// The screens are driven with the keys of a TV remote, the way a person uses them.

/** Presses [key] on the remote, on whatever has focus. */
fun ComposeContentTestRule.press(key: Key) {
    onRoot().performKeyInput { pressKey(key) }
    waitForIdle()
}

/** Moves focus to this node, then presses OK. */
fun SemanticsNodeInteraction.focusAndPressOk(rule: ComposeContentTestRule) {
    requestFocus()
    rule.press(Key.DirectionCenter)
}
