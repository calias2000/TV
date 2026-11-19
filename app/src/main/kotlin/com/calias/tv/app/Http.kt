package com.calias.tv.app

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val CONNECT_TIMEOUT = 15.seconds

// The blocking request runs in a scope of its own, not in the caller's. The connection timeouts do not
// cover the DNS lookup, which can hang for minutes on a bad network. This way the caller gives up after
// its time budget while the stuck thread is left to finish by itself.
private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * GET [url] and hand the response body to [read]. The whole request (lookup, connect, download and [read])
 * gets [timeout]; after that it fails with a readable message ("Playlist server did not answer in time").
 * Also fails with ("[what] server answered HTTP 500") unless the answer is 200. A 404 says what it usually
 * means: a link that is wrong, expired (an access code) or not ready yet (its playlist is not built).
 * [headers] are sent with the request. Used by the playlist, guide and update downloads.
 */
internal suspend fun <T> httpGet(
    url: String,
    what: String,
    timeout: Duration,
    headers: Map<String, String> = emptyMap(),
    read: (InputStream) -> T,
): T {
    val request = ioScope.async {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT.inWholeMilliseconds.toInt()
            connection.readTimeout = timeout.inWholeMilliseconds.toInt()
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) {
                if (code == HttpURLConnection.HTTP_NOT_FOUND) "$what not found (HTTP 404): the link may be wrong, expired or not ready yet"
                else "$what server answered HTTP $code"
            }
            connection.inputStream.buffered().use(read)
        } finally {
            connection.disconnect()
        }
    }
    try {
        return withTimeout(timeout) { request.await() }
    } catch (e: TimeoutCancellationException) {
        request.cancel()
        throw IOException("$what server did not answer in time", e)
    } catch (e: SocketTimeoutException) {
        // The connection's own read timer can run out just before the budget does: same message.
        throw IOException("$what server did not answer in time", e)
    }
}
