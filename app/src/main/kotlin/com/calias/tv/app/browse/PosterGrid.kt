package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.calias.tv.app.ui.Corner
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassSurface
import com.calias.tv.app.ui.PictureGlow
import com.calias.tv.app.ui.ProgressBar
import com.calias.tv.app.ui.Space
import com.calias.tv.app.ui.fadeEdges

/**
 * The scrolling grid behind the Movies and Series screens, with [columns] cards per row. Like
 * [BrowseList]: focus goes once to the card the user was on last (the first, at first), which is kept
 * when the screen is left, so coming back from the player lands on the same card. [card] draws one
 * card and must apply the [Modifier] it gets.
 */
@Composable
fun PosterGrid(count: Int, columns: Int, card: @Composable (index: Int, focus: Modifier) -> Unit) {
    var last by rememberSaveable { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = last - last % columns)
    val requester = remember { FocusRequester() }
    // The card only exists after the first frame, so wait for it before asking for focus.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        requester.requestFocus()
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize().fadeEdges(),
        contentPadding = PaddingValues(vertical = Space.Gap),
        verticalArrangement = Arrangement.spacedBy(Space.Gap),
        horizontalArrangement = Arrangement.spacedBy(Space.Gap),
    ) {
        items(count) { index ->
            val tracking = Modifier.onFocusChanged { if (it.hasFocus) last = index }
            card(index, if (index == last) tracking.focusRequester(requester) else tracking)
        }
    }
}

/**
 * One card of a [PosterGrid]: the picture (shaped by [aspect], width over height) in a glass frame,
 * and the title under it, with an optional muted [subtitle]. [progress] draws how far a movie was
 * watched along the picture's bottom edge. While focused, the background glows in the picture's
 * main color, or [fallbackTint] when it has none. [modifier] goes on the glass frame (the grid's focus
 * modifier belongs there). No focus zoom, so cards never spill past the margin.
 */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    aspect: Float,
    fallbackTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
) {
    var focused by remember { mutableStateOf(false) }
    PictureGlow(focused, imageUrl, fallbackTint)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassSurface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().aspectRatio(aspect).onFocusChanged { focused = it.hasFocus },
            shape = Corner.Medium,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null, // decorative: the title is under it
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (progress != null) ProgressBar(progress, Modifier.align(Alignment.BottomCenter).padding(10.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Glass.colors.textMuted, maxLines = 1)
        }
    }
}
