package com.calias.tv.app

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The download helper, against a tiny server that lives inside the test (localhost only, no internet). */
class HttpGetTest {

    private lateinit var server: HttpServer

    private fun url(path: String) = "http://localhost:${server.address.port}$path"

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/ok") { exchange ->
            val body = "hello".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.createContext("/broken") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        server.createContext("/slow") { exchange ->
            Thread.sleep(3_000) // longer than the time the test allows
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    @Test
    fun `hands the body to the reader`() = runBlocking {
        val text = httpGet(url("/ok"), "Test", 5.seconds) { it.bufferedReader().readText() }
        assertEquals("hello", text)
    }

    @Test
    fun `an answer other than 200 fails with a readable message, a 404 says what it usually means`() {
        fun failure(path: String) = assertFailsWith<IllegalStateException> {
            runBlocking { httpGet(url(path), "Playlist", 5.seconds) { it.readBytes() } }
        }.message
        assertEquals("Playlist server answered HTTP 500", failure("/broken"))
        assertEquals("Playlist not found (HTTP 404): the link may be wrong, expired or not ready yet", failure("/missing"))
    }

    @Test
    fun `a server that does not answer in time fails after the time budget`() {
        val started = System.nanoTime()
        val error = assertFailsWith<IOException> {
            runBlocking { httpGet(url("/slow"), "Guide", 300.milliseconds) { it.readBytes() } }
        }
        assertEquals("Guide server did not answer in time", error.message)
        assertTrue((System.nanoTime() - started) / 1_000_000 < 2_500, "gave up long before the server answered")
    }
}
