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
    fun `a source survives encode and decode, and unreadable text gives null`() {
        val source = Source.Direct("https://a.example/get.php?username=u&password=p")
        assertEquals(source, Source.decode(Source.encode(source)))
        assertNull(Source.decode("linked:7"))
        assertNull(Source.decode("direct:no url"))
    }
}
