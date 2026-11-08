package com.calias.tv.core.library

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

/** Where a channel sits in a list of categories: the category index and the index inside it. */
data class Position(val group: Int, val index: Int)

/** How many live channels, movies and episodes the playlist has. */
data class Counts(val live: Int, val movies: Int, val episodes: Int)

/** A named group of live channels, such as "NOTICIAS" or "DAZN". */
data class Category(val name: String, val channels: List<Channel>)

/**
 * Splits the flat playlist into the three browsable sections.
 *
 * Live channels are grouped by their `group-title`, in the order the playlist first mentions each
 * group. Movies and series stay flat, in playlist order. Pure logic, no Android.
 */
object Library {

    /** Name for live channels that have no `group-title`. */
    private const val UNGROUPED = "Other"

    fun liveCategories(channels: List<Channel>): List<Category> =
        channels
            .filter { it.kind == ContentKind.LIVE }
            .groupBy { it.group?.takeIf(String::isNotBlank) ?: UNGROUPED }
            .map { (name, group) -> Category(name, group) }

    fun movies(channels: List<Channel>): List<Channel> =
        channels.filter { it.kind == ContentKind.MOVIE }

    fun series(channels: List<Channel>): List<Channel> =
        channels.filter { it.kind == ContentKind.SERIES }

    /** Where the channel with [streamUrl] is in [categories], or null if it is not there (or the url is null). */
    fun locate(categories: List<Category>, streamUrl: String?): Position? {
        if (streamUrl == null) return null
        categories.forEachIndexed { group, category ->
            val index = category.channels.indexOfFirst { it.streamUrl == streamUrl }
            if (index >= 0) return Position(group, index)
        }
        return null
    }

    fun counts(channels: List<Channel>): Counts = Counts(
        live = channels.count { it.kind == ContentKind.LIVE },
        movies = channels.count { it.kind == ContentKind.MOVIE },
        episodes = channels.count { it.kind == ContentKind.SERIES },
    )
}
