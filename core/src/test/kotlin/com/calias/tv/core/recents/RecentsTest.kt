package com.calias.tv.core.recents

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals

class RecentsTest {

    private fun ch(url: String, kind: ContentKind = ContentKind.LIVE) =
        Channel(id = url, name = url, streamUrl = url, group = null, kind = kind)

    @Test
    fun `newest goes first, a repeat moves up, and the list is capped`() {
        assertEquals(listOf("b", "a"), Recents.add(listOf("a"), "b"))
        assertEquals(listOf("a", "b"), Recents.add(listOf("b", "a"), "a"))
        assertEquals(listOf("f", "e", "d"), Recents.add(listOf("e", "d", "c"), "f", limit = 3))
    }

    @Test
    fun `resolve keeps the order of the urls and drops what the playlist no longer has, and non-live entries`() {
        val channels = listOf(ch("a"), ch("b"), ch("movie", ContentKind.MOVIE))
        assertEquals(listOf("b", "a"), Recents.resolve(listOf("b", "gone", "movie", "a"), channels).map { it.id })
    }

    @Test
    fun `encode and decode round trip`() {
        val list = listOf("http://x/1", "http://x/2")
        assertEquals(list, Recents.decode(Recents.encode(list)))
        assertEquals(emptyList(), Recents.decode(""))
    }
}
