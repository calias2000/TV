package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.browse.PosterCard
import com.calias.tv.app.browse.PosterGrid
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.SectionTint
import com.calias.tv.app.ui.Space
import com.calias.tv.core.Channel
import com.calias.tv.core.resume.Resume
import com.calias.tv.core.resume.ResumeRecord
import com.calias.tv.core.series.Show
import com.calias.tv.core.series.Shows

// The Movies and Series screens: movies as a grid of posters, series as a grid of shows, and a list
// of episodes inside a show.

/** The frame of the three screens: the title with the number of items, then [content], or a hint when there are none. */
@Composable
private fun Shelf(title: String, count: Int, noun: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(Edge.Margin)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Gap), verticalAlignment = Alignment.Bottom) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(text = countOf(count, noun), color = Glass.colors.textMuted)
        }
        if (count == 0) Text(text = "Nothing to show. Check Settings.", modifier = Modifier.padding(top = 24.dp)) else content()
    }
}

/** Movies: a grid of posters. A movie that was started shows how far it got. */
@Composable
fun MoviesScreen(movies: List<Channel>, resume: Map<String, ResumeRecord>, onWatch: (channelId: String) -> Unit) {
    Shelf("Movies", movies.size, "movie") {
        PosterGrid(count = movies.size, columns = 6) { index, focus ->
            val movie = movies[index]
            PosterCard(
                title = movie.name,
                imageUrl = movie.logo,
                aspect = 2f / 3f,
                fallbackTint = SectionTint.Movies,
                modifier = focus,
                onClick = { onWatch(movie.id) },
                progress = Resume.progress(resume, movie),
            )
        }
    }
}

/** Series: a grid of shows. OK opens the show's episodes. */
@Composable
fun ShowsScreen(shows: List<Show>, onOpen: (Show) -> Unit) {
    Shelf("Series", shows.size, "show") {
        PosterGrid(count = shows.size, columns = 4) { index, focus ->
            val show = shows[index]
            PosterCard(
                title = show.name,
                imageUrl = show.logo,
                aspect = 16f / 9f,
                fallbackTint = SectionTint.Series,
                modifier = focus,
                onClick = { onOpen(show) },
                subtitle = countOf(show.episodes.size, "episode"),
            )
        }
    }
}

/** The episodes of one show, opened on the one that was watched last. */
@Composable
fun EpisodesScreen(show: Show, resume: Map<String, ResumeRecord>, onWatch: (channelId: String) -> Unit) {
    Shelf(show.name, show.episodes.size, "episode") {
        val start = remember(show) { show.episodes.indexOfFirst { Resume.progress(resume, it) != null }.coerceAtLeast(0) }
        BrowseList(count = show.episodes.size, focusIndex = start) { index, focus ->
            val episode = show.episodes[index]
            ChannelRow(
                title = Shows.episodeOf(episode.name),
                channel = episode,
                progress = Resume.progress(resume, episode),
                modifier = focus,
                onClick = { onWatch(episode.id) },
            )
        }
    }
}
