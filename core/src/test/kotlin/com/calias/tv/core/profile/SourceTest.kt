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
        val link = Source.Direct("http://pc:5002/api/iptv/p/AB23/list")
        assertEquals(link, Source.parse("AB23", "http://pc:5002"))
        assertEquals(link, Source.parse(" ab-2 3\n", "http://pc:5002/"), "case, spaces, dashes and a final slash do not matter")
        assertEquals(Source.Direct("https://a.example/x.m3u"), Source.parse("https://a.example/x.m3u", "http://pc:5002"))
    }

    @Test
    fun `what is neither an address nor a code is refused, and so is a code without a server`() {
        listOf("", "AB2", "AB234", "AB01", "ABIO", "a.example/list", "AB 2").forEach { assertNull(Source.parse(it, "http://pc:5002"), "'$it'") }
        assertNull(Source.parse("AB23", ""))
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
