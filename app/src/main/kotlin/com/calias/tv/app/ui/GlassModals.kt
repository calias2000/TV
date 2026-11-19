package com.calias.tv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private val DIALOG_WIDTH = 640.dp
private val DIALOG_MAX_HEIGHT = 480.dp
private val SHEET_WIDTH = 360.dp

/**
 * What both modals share: a platform dialog window, so focus stays inside it, BACK calls [onDismiss], and
 * the keyboard behaves. A window costs more than a plain overlay, but a modal is open only for short tasks,
 * never over playing video. The glass panel, sized by [modifier], sits at [alignment] over a dimmed screen,
 * inside the screen margin, with the same padding and spacing in both.
 */
@Composable
private fun GlassWindow(onDismiss: () -> Unit, alignment: Alignment, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(Edge.Margin), contentAlignment = alignment) {
            GlassPanel(modifier = modifier, fill = Glass.colors.backgroundTop, edge = Glass.colors.edge) {
                Column(modifier = Modifier.padding(Space.Panel), verticalArrangement = Arrangement.spacedBy(Space.Gap), content = content)
            }
        }
    }
}

/** The [title] of a modal and, at its right, one optional [action] (a button), as tall as a button. */
@Composable
private fun ModalHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        action?.invoke()
    }
}

/**
 * The modal for a task: a panel with a [title], at the top of the screen, with one optional [action] at the
 * right of the title. Its top edge is always at the screen margin, so none jumps from one to the next. It is
 * at the top, not centered, because the TV keyboard covers the lower middle of the screen without telling the
 * app (no window insets) and would hide the fields of a form. [content] scrolls when it is taller than the panel.
 */
@Composable
fun GlassDialog(
    title: String,
    onDismiss: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassWindow(onDismiss, Alignment.TopCenter, Modifier.width(DIALOG_WIDTH).heightIn(max = DIALOG_MAX_HEIGHT)) {
        ModalHeader(title, action = action)
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(vertical = Space.FocusEdge),
            verticalArrangement = Arrangement.spacedBy(Space.Gap),
            content = content,
        )
    }
}

/**
 * The modal for a choice: a panel on the right, as tall as the screen, like the cards of the player. [title]
 * on top, an optional [footer] (a button) at the bottom, and [content] between them, which has to scroll by
 * itself (a BrowseList whose rows are inset by Space.FocusEdge).
 */
@Composable
fun GlassSideSheet(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassWindow(onDismiss, Alignment.CenterEnd, Modifier.width(SHEET_WIDTH).fillMaxHeight()) {
        // The list inside keeps Space.FocusEdge free at its sides, so the title and footer keep it too and everything lines up.
        ModalHeader(title, Modifier.padding(horizontal = Space.FocusEdge))
        Box(modifier = Modifier.weight(1f)) { content() }
        footer?.let { Box(modifier = Modifier.padding(horizontal = Space.FocusEdge)) { it() } }
    }
}
