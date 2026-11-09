package com.calias.tv.core.series

import com.calias.tv.core.Channel

/** One show: its name and its episodes, in playlist order. */
data class Show(val name: String, val episodes: List<Channel>) {

    /** A picture for the show: the first episode that has one. */
    val logo: String? get() = episodes.firstNotNullOfOrNull { it.logo?.takeIf(String::isNotBlank) }
}

/**
 * Episode titles look like "Show Name (2011) (US) - S01E01 - Pilot". The playlist has no season or
 * show data, so the show and the episode are read from the title. Pure logic, no Android.
 */
object Shows {

    // Group 1 is the show, group 2 the episode.
    private val marker = Regex("""^(.+?) - (S\d+E\d+\b.*)$""")

    /** The show name in an episode [title], or null if the title has no "S01E01" part. */
    fun showOf(title: String): String? = marker.find(title)?.groupValues?.get(1)

    /** The episode part of [title] ("S01E01 - Pilot"), or the whole title if it has none. */
    fun episodeOf(title: String): String = marker.find(title)?.groupValues?.get(2) ?: title

    /** Groups [episodes] by show, in the order each show first appears. A title without "S01E01" is a show of its own. */
    fun group(episodes: List<Channel>): List<Show> =
        episodes.groupBy { showOf(it.name) ?: it.name }.map { (name, list) -> Show(name, list) }
}
