package com.calias.tv.app.update

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The APK download, against a tiny server inside the test (localhost only). */
class ApkDownloadTest {

    private lateinit var server: HttpServer
    private val bytes = ByteArray(200_000) { (it % 251).toByte() }
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val target = File.createTempFile("update", ".apk").also { it.delete(); it.deleteOnExit() }

    private fun release(path: String = "/apk", checksum: String? = sha256) =
        Release("0.2.0", "http://localhost:${server.address.port}$path", bytes.size.toLong(), checksum)

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/apk") { exchange ->
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/missing") { exchange -> exchange.sendResponseHeaders(404, -1); exchange.close() }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    @Test
    fun `the file arrives whole, with the progress rising to 100`() {
        val progress = mutableListOf<Int>()
        runBlocking { downloadApk(release(), target) { progress += it } }

        assertTrue(bytes.contentEquals(target.readBytes()))
        assertEquals(100, progress.last())
        assertEquals(progress.sorted(), progress)
    }

    @Test
    fun `a file that is not the one GitHub described is deleted and refused`() {
        val error = assertFailsWith<IOException> { runBlocking { downloadApk(release(checksum = "0".repeat(64)), target) {} } }
        assertEquals("The download is damaged: its checksum is not the release's", error.message)
        assertFalse(target.exists())
    }

    @Test
    fun `with no checksum from GitHub the file is kept`() {
        runBlocking { downloadApk(release(checksum = null), target) {} }
        assertTrue(target.exists())
    }

    @Test
    fun `a failed download leaves nothing behind`() {
        target.writeText("an older download")
        assertFailsWith<IllegalStateException> { runBlocking { downloadApk(release("/missing"), target) {} } }
        assertFalse(target.exists())
    }
}
