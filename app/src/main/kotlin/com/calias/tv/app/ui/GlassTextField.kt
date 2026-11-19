package com.calias.tv.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * A one-line glass text field for [state], with a [label] above it, as tall as a button. [modifier] places
 * the label and field together; [focusRequester] reaches the field itself. Like every glass control, focus
 * strengthens its edge.
 * The keyboard opens when the field gets focus; [onDone] runs when its Done key is pressed.
 */
@Composable
fun GlassTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit,
) {
    val colors = Glass.colors
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
        BasicTextField(
            state = state,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = LocalTextStyle.current.copy(color = colors.text),
            cursorBrush = SolidColor(colors.text),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            onKeyboardAction = { performDefault ->
                performDefault()
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .background(if (focused) colors.fillFocused else colors.fill, Corner.Medium)
                .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) colors.edgeFocused else colors.edge), Corner.Medium)
                .padding(horizontal = Space.Inner),
            decorator = { innerTextField ->
                Box(modifier = Modifier.height(Space.Control), contentAlignment = Alignment.CenterStart) { innerTextField() }
            },
        )
    }
}
