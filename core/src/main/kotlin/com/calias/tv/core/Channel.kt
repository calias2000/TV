package com.calias.tv.core

/**
 * One entry read from a playlist: a live channel, a movie, or a series episode.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val group: String?,
    val logo: String? = null,
    val tvgId: String? = null,
    val kind: ContentKind = ContentKind.LIVE,
)

/** What kind of content a playlist entry is. Read from the tvg-type attribute. */
enum class ContentKind { LIVE, MOVIE, SERIES }
