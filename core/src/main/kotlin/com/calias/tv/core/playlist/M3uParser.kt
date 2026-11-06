package com.calias.tv.core.playlist

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

/** Result of reading one M3U playlist: the EPG URL from its header, plus every entry. */
data class Playlist(
    val epgUrl: String?,
    val channels: List<Channel>,
)

/**
 * Reads M3U / M3U8 text.
 *
 * Understands: the #EXTM3U header (url-tvg / x-tvg-url), #EXTINF lines (attributes and display
 * name), and the stream URL on the line after each #EXTINF.
 *
 * Known limits, on purpose for now:
 * - Reads the whole text into memory. Fine for a few thousand entries. Switch to line-by-line
 *   reading if playlists grow to tens of thousands.
 * - Ignores per-channel HTTP headers (#EXTVLCOPT, #EXTHTTP) and DRM lines (#KODIPROP).
 * - A URL line without a preceding #EXTINF is skipped.
 */
object M3uParser {

    private val attributePattern = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(text: String): Playlist {
        var epgUrl: String? = null
        val channels = mutableListOf<Channel>()
        var pendingAttributes: Map<String, String>? = null
        var pendingDisplayName = ""

        for (raw in text.removePrefix("﻿").lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U") -> {
                    val attributes = readAttributes(line)
                    epgUrl = attributes["url-tvg"] ?: attributes["x-tvg-url"]
                }
                line.startsWith("#EXTINF") -> {
                    pendingAttributes = readAttributes(line)
                    pendingDisplayName = readDisplayName(line)
                }
                line.startsWith("#") -> Unit
                else -> {
                    val attributes = pendingAttributes
                    if (attributes != null) {
                        channels += toChannel(index = channels.size, url = line, attributes = attributes, displayName = pendingDisplayName)
                    }
                    pendingAttributes = null
                    pendingDisplayName = ""
                }
            }
        }
        return Playlist(epgUrl = epgUrl, channels = channels)
    }

    private fun toChannel(index: Int, url: String, attributes: Map<String, String>, displayName: String): Channel {
        val kind = when (attributes["tvg-type"] ?: attributes["type"]) {
            "series" -> ContentKind.SERIES
            "movie", "vod" -> ContentKind.MOVIE
            else -> ContentKind.LIVE
        }
        return Channel(
            // Position-based id: unique even when tvg-id values repeat, which they do in real playlists.
            id = "entry-$index",
            name = displayName.ifBlank { attributes["tvg-name"] ?: url },
            streamUrl = url,
            group = attributes["group-title"],
            logo = attributes["tvg-logo"],
            tvgId = attributes["tvg-id"],
            kind = kind,
        )
    }

    /** The key="value" pairs before the display-name comma. */
    private fun readAttributes(line: String): Map<String, String> =
        attributePattern.findAll(headOf(line))
            .associate { it.groupValues[1] to it.groupValues[2] }

    /** The text after the first comma that is not inside quotes. */
    private fun readDisplayName(line: String): String {
        val index = commaOutsideQuotes(line)
        return if (index < 0) "" else line.substring(index + 1).trim()
    }

    private fun headOf(line: String): String {
        val index = commaOutsideQuotes(line)
        return if (index < 0) line else line.substring(0, index)
    }

    private fun commaOutsideQuotes(line: String): Int {
        var inQuotes = false
        for ((i, c) in line.withIndex()) {
            if (c == '"') inQuotes = !inQuotes
            if (c == ',' && !inQuotes) return i
        }
        return -1
    }
}
