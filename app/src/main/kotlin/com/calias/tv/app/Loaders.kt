package com.calias.tv.app

import android.util.Xml
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.epg.Xmltv
import com.calias.tv.core.playlist.M3uParser
import com.calias.tv.core.playlist.Playlist
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/** Downloads the playlist text over HTTPS and parses it with the core parser. */
object PlaylistLoader {

    const val PLAYLIST_URL = "https://calias.me/api/iptv/live/2"

    private val TIMEOUT = 20.seconds

    suspend fun load(): Playlist = httpGet(PLAYLIST_URL, "Playlist", TIMEOUT) { stream ->
        M3uParser.parse(stream.bufferedReader(Charsets.UTF_8).readText())
    }
}

/**
 * Downloads the XMLTV guide and parses it while it streams in. Keeps only the programs of
 * `wantedIds` from one hour ago to 24 hours ahead.
 */
object EpgLoader {

    // The guide is about 11 MB, bigger than the playlist, so it gets a longer time budget.
    private val TIMEOUT = 90.seconds

    suspend fun load(epgUrl: String, wantedIds: Set<String>, nowMs: Long): Guide =
        httpGet(epgUrl, "Guide", TIMEOUT) { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, null) // null: the parser reads the encoding from the XML header
            Xmltv.parse(parser, wantedIds, nowMs - 1.hours.inWholeMilliseconds, nowMs + 24.hours.inWholeMilliseconds)
        }
}
