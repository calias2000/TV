package com.calias.tv.app

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

private const val CONNECT_TIMEOUT_MS = 15_000

// The blocking request runs in a scope of its own, not in the caller's. The connection timeouts do not
// cover the DNS lookup, which can hang for minutes on a bad network. This way the caller gives up after
// its time budget while the stuck thread is left to finish by itself.
private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * GET [url] and hand the response body to [read]. The whole request (lookup, connect, download and [read])
 * gets [timeoutMs]; after that it fails with a readable message ("Playlist server did not answer in time").
 * Also fails with ("[what] server answered HTTP 404") unless the answer is 200.
 * Used by the playlist and guide loaders.
 */
internal suspend fun <T> httpGet(url: String, what: String, timeoutMs: Long, read: (InputStream) -> T): T {
    val request = ioScope.async {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = timeoutMs.toInt()
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "$what server answered HTTP $code" }
            connection.inputStream.buffered().use(read)
        } finally {
            connection.disconnect()
        }
    }
    try {
        return withTimeout(timeoutMs) { request.await() }
    } catch (e: TimeoutCancellationException) {
        request.cancel()
        throw IOException("$what server did not answer in time", e)
    }
}
