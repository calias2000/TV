package com.calias.tv.app

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CONNECT_TIMEOUT_MS = 15_000

/**
 * GET [url] on Dispatchers.IO and hand the response body to [read]. Fails with a readable message
 * ("[what] server answered HTTP 404") unless the answer is 200. Used by the playlist and guide loaders.
 */
internal suspend fun <T> httpGet(url: String, what: String, readTimeoutMs: Int, read: (InputStream) -> T): T =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = readTimeoutMs
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "$what server answered HTTP $code" }
            connection.inputStream.buffered().use(read)
        } finally {
            connection.disconnect()
        }
    }
