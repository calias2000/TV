package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.calias.tv.app.ui.fadeEdges

/**
 * The scrolling list behind every browse view. It starts scrolled to [focusIndex]. [focusRequester] is
 * always on the row that had focus last (at first, the row at [focusIndex]), so the caller can send
 * focus back to where the user was. Focus goes there once, when the list first appears. [row] draws
 * one row and must apply the [Modifier] it gets.
 */
@Composable
fun BrowseList(
    count: Int,
    focusIndex: Int,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    row: @Composable (index: Int, focus: Modifier) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (focusIndex - 3).coerceAtLeast(0))
    var lastFocused by remember { mutableIntStateOf(focusIndex) }
    // The row only exists after the first frame, so wait for it before asking for focus.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().fadeEdges(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(count) { index ->
            val tracking = Modifier.onFocusChanged { if (it.hasFocus) lastFocused = index }
            row(index, if (index == lastFocused) tracking.focusRequester(focusRequester) else tracking)
        }
    }
}
