package com.calias.tv.app

import com.calias.tv.core.playlist.M3uParser
import com.calias.tv.core.playlist.Playlist
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads the playlist text over HTTPS and parses it with the core parser.
 * Network work runs on Dispatchers.IO, so the UI thread never waits for it.
 */
object PlaylistLoader {

    // Your playlist endpoint. HTTPS, so Android allows it without a cleartext exception.
    const val PLAYLIST_URL = "https://calias.me/api/iptv/live/1"

    private const val TIMEOUT_MS = 15_000

    suspend fun load(): Playlist = withContext(Dispatchers.IO) {
        val connection = URL(PLAYLIST_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "Playlist server answered HTTP $code" }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            M3uParser.parse(text)
        } finally {
            connection.disconnect()
        }
    }
}
