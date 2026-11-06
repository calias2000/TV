package com.calias.tv.core.library

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

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
}
