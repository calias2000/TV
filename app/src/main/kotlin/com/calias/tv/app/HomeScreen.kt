package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.browse.ProgressBar
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassIconButton
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.GlassSurface
import com.calias.tv.app.ui.LocalBackdrop
import com.calias.tv.app.ui.SectionTint
import com.calias.tv.app.ui.Spinner
import com.calias.tv.app.ui.rememberPictureColor
import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.library.Counts
import com.calias.tv.core.resume.ContinueItem
import com.calias.tv.core.seek.Seek

/**
 * Home: a quiet clock and three glass icon buttons on top, three glass tiles with counts, and a small
 * centered row under them that follows the tile in focus: recent live channels under Live TV, movies
 * you started under Movies, series you started under Series (hidden when there is nothing).
 * Status shows only while the playlist is loading or failed. Everything has to fit a 540 dp tall screen.
 *
 * Tiles stay dimmed until a playlist is loaded; when it loads, focus moves to the tile used last (the
 * first one on a fresh start). The background glows in the focused tile's color, or in the main color
 * of the focused card's picture.
 */
@Composable
fun HomeScreen(
    state: PlaylistState,
    counts: Counts?,
    resume: List<ContinueItem>,
    recentLive: List<Channel>,
    guide: Guide,
    onResume: (ContinueItem) -> Unit,
    onRecentLive: (Channel) -> Unit,
    onOpen: (Any) -> Unit,
    onSync: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val ready = state is PlaylistState.Loaded
    val nowMs = rememberNowMs()
    fun resumeCards(kind: ContentKind) = resume.filter { it.channel.kind == kind }.map { item ->
        CardSpec(item.channel, "Resume at ${Seek.formatClock(item.positionMs)}", null) { onResume(item) }
    }
    val liveCards = recentLive.map { channel ->
        val now = guide.nowNext(channel.tvgId, nowMs)?.now
        CardSpec(channel, now?.title, now?.progress(nowMs)) { onRecentLive(channel) }
    }
    val tiles = listOf(
        HomeTileSpec("Live TV", LiveTv(recentLive.firstOrNull()?.streamUrl), counts?.let { count(it.live, "channel") }, SectionTint.Live, "Recently watched", liveCards),
        HomeTileSpec("Movies", MovieList, counts?.let { count(it.movies, "movie") }, SectionTint.Movies, "Continue watching", resumeCards(ContentKind.MOVIE)),
        HomeTileSpec("Series", SeriesList, counts?.let { count(it.episodes, "episode") }, SectionTint.Series, "Continue watching", resumeCards(ContentKind.SERIES)),
    )
    val tileFocus = remember { List(tiles.size) { FocusRequester() } }
    var section by rememberSaveable { mutableIntStateOf(0) } // the tile in focus, or used last
    val backdrop = LocalBackdrop.current
    // The tiles cannot take focus while loading, so focus waits for the playlist.
    LaunchedEffect(ready) { if (ready) tileFocus[section].requestFocus() }
    DisposableEffect(Unit) { onDispose { backdrop.color = null } }

    val cards = tiles[section].cards

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = Edge.Margin),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Edge.Margin),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Clock(nowMs, modifier = Modifier.weight(1f))
                GlassIconButton(R.drawable.ic_sync, "Sync", onSync)
                GlassIconButton(R.drawable.ic_settings, "Settings", { onOpen(Settings) })
                GlassIconButton(R.drawable.ic_theme, "Switch theme", onToggleTheme)
            }
            // The tiles and the row under them sit in the middle of what is left. The row has a fixed
            // height, even when empty, so the tiles never jump when you move between tiles.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Edge.Margin),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    tiles.forEachIndexed { index, tile ->
                        HomeTile(
                            label = tile.label,
                            count = tile.count,
                            ready = ready,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(tileFocus[index])
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        section = index
                                        backdrop.color = tile.tint
                                    }
                                },
                        ) { onOpen(tile.destination) }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().height(CARDS_AREA),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (cards.isNotEmpty()) {
                        Text(
                            text = tiles[section].cardsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Glass.colors.textMuted,
                        )
                        // The row spans the whole screen width (so cards that scroll away are not cut at the
                        // margin); the margin is its content padding.
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = Edge.Margin),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            items(cards, key = { it.channel.id }) { card ->
                                ContinueCard(card = card, fallbackTint = tiles[section].tint)
                            }
                        }
                    }
                }
            }
        }
        StatusPill(state, Modifier.align(Alignment.BottomCenter).padding(Edge.Margin))
    }
}

/** Room for the "Continue watching" title and one card, so the layout above it does not move. */
private val CARDS_AREA = 120.dp

/** "1 movie", "2 movies". */
private fun count(n: Int, noun: String) = if (n == 1) "$n $noun" else "$n ${noun}s"

/** One Home tile: what it shows, where it leads, its backdrop color while focused, and the cards shown under it. */
private class HomeTileSpec(
    val label: String,
    val destination: Any,
    val count: String?,
    val tint: Color,
    val cardsTitle: String,
    val cards: List<CardSpec>,
)

/** One card of the row under the tiles: a channel, movie or episode, a line under its name, and what OK does. */
private class CardSpec(val channel: Channel, val subtitle: String?, val progress: Float?, val onClick: () -> Unit)

/** The time, large and light, like a TV launcher. [nowMs] comes from [rememberNowMs]. */
@Composable
private fun Clock(nowMs: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatTime(nowMs),
        style = MaterialTheme.typography.displaySmall,
        modifier = modifier,
    )
}

/**
 * A large glass tile. When not ready it is dimmed and cannot take focus, so pressing it does
 * nothing and the remote skips it.
 */
@Composable
private fun HomeTile(label: String, count: String?, ready: Boolean, modifier: Modifier, onClick: () -> Unit) {
    GlassSurface(
        onClick = onClick,
        modifier = modifier
            .height(150.dp)
            .alpha(if (ready) 1f else 0.35f)
            .focusProperties { canFocus = ready },
    ) {
        Column(
            modifier = Modifier.padding(24.dp).align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.headlineMedium)
            if (count != null) {
                Text(text = count, style = MaterialTheme.typography.bodyMedium, color = Glass.colors.textMuted)
            }
        }
    }
}

/**
 * A compact glass card: the picture, the name, and one line (what is on now, or where it resumes).
 * While it has focus the background glows in the main color of its picture. That color arrives a moment
 * after the card is first focused, so it is applied when it is ready, not only when focus changes.
 */
@Composable
private fun ContinueCard(card: CardSpec, fallbackTint: Color) {
    val backdrop = LocalBackdrop.current
    val tint = rememberPictureColor(card.channel.logo)
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, tint) { if (focused) backdrop.color = tint ?: fallbackTint }
    GlassSurface(onClick = card.onClick, modifier = Modifier.width(260.dp).onFocusChanged { focused = it.hasFocus }) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogo(card.channel)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.channel.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (card.subtitle != null) {
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Glass.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.progress != null) ProgressBar(card.progress, Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * Shown only while the playlist loads (a spinner) or when it failed (the reason), at the bottom of the
 * screen so nothing moves when it appears or goes. Not interactive, so it never takes focus. When all
 * is well, nothing shows.
 */
@Composable
private fun StatusPill(state: PlaylistState, modifier: Modifier = Modifier) {
    if (state is PlaylistState.Loaded) return
    GlassPanel(modifier = modifier, fill = Glass.colors.fill, edge = Glass.colors.edge) {
        val text = statusText(state)
        if (text == null) {
            Spinner(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), diameter = 24.dp)
        } else {
            Text(text = text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), maxLines = 2)
        }
    }
}
