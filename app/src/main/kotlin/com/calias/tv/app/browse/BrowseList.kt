package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * The scrolling list behind every browse view. It starts scrolled to [focusIndex], and the row of that
 * index gets [focusRequester], so the caller can send focus there. With [autoFocus], focus goes
 * there once, when the list first appears. [row] draws one row and must apply the [Modifier] it gets.
 */
@Composable
fun BrowseList(
    count: Int,
    focusIndex: Int,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    row: @Composable (index: Int, focus: Modifier) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (focusIndex - 3).coerceAtLeast(0))
    if (autoFocus) {
        // The row only exists after the first frame, so wait for it before asking for focus.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(count) { index ->
            row(index, if (index == focusIndex) Modifier.focusRequester(focusRequester) else Modifier)
        }
    }
}
