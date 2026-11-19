package com.calias.tv.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceTest {

    @Test
    fun `an http or https URL with a host is a direct source, trimmed`() {
        assertEquals(Source.Direct("https://a.example/get.php?u=1"), Source.direct("  https://a.example/get.php?u=1 \n"))
        assertEquals(Source.Direct("http://10.0.0.5:8080/list.m3u"), Source.direct("http://10.0.0.5:8080/list.m3u"))
    }

    @Test
    fun `anything else is refused`() {
        listOf("", "   ", "a.example/list", "ftp://a.example/x", "file:///sdcard/x.m3u", "https://", "https://a b.example/x", "not a url")
            .forEach { assertNull(Source.direct(it), "'$it'") }
    }

    @Test
    fun `a code becomes the playlist link of that code on the server`() {
        val link = Source.Direct("http://pc:5002/api/iptv/p/012345/list")
        assertEquals(link, Source.parse("012345", "http://pc:5002"), "leading zeros stay")
        assertEquals(link, Source.parse(" 012-34 5\n", "http://pc:5002/"), "spaces, dashes and a final slash do not matter")
        assertEquals(Source.Direct("https://a.example/x.m3u"), Source.parse("https://a.example/x.m3u", "http://pc:5002"))
    }

    @Test
    fun `what is neither an address nor a code is refused, and so is a code without a server`() {
        listOf("", "12345", "1234567", "12345a", "ABCDEF", "a.example/list", "12 34").forEach { assertNull(Source.parse(it, "http://pc:5002"), "'$it'") }
        assertNull(Source.parse("012345", ""))
        assertNull(Source.parse("  ", "http://pc:5002"))
    }

    @Test
    fun `a source survives encode and decode, and unreadable text gives null`() {
        val source = Source.Direct("https://a.example/get.php?username=u&password=p")
        assertEquals(source, Source.decode(Source.encode(source)))
        assertNull(Source.decode("linked:7"))
        assertNull(Source.decode("direct:no url"))
    }
}
