package com.calias.tv.app

import com.calias.tv.core.playlist.M3uParser
import com.calias.tv.core.playlist.Playlist
import kotlin.time.Duration.Companion.seconds

/** Downloads the playlist text over HTTPS and parses it with the core parser. */
object PlaylistLoader {

    // Playlist endpoint.
    const val PLAYLIST_URL = "https://calias.me/api/iptv/live/2"

    private val TIMEOUT = 20.seconds

    suspend fun load(): Playlist = httpGet(PLAYLIST_URL, "Playlist", TIMEOUT) { stream ->
        M3uParser.parse(stream.bufferedReader(Charsets.UTF_8).readText())
    }
}
