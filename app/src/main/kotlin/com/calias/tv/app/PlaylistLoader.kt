package com.calias.tv.app

import com.calias.tv.core.playlist.M3uParser
import com.calias.tv.core.playlist.Playlist

/** Downloads the playlist text over HTTPS and parses it with the core parser. */
object PlaylistLoader {

    // Playlist endpoint.
    const val PLAYLIST_URL = "https://calias.me/api/iptv/live/1"

    private const val READ_TIMEOUT_MS = 15_000

    suspend fun load(): Playlist = httpGet(PLAYLIST_URL, "Playlist", READ_TIMEOUT_MS) { stream ->
        M3uParser.parse(stream.bufferedReader(Charsets.UTF_8).readText())
    }
}
