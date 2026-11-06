package com.calias.tv.core.library

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryTest {

    private fun ch(id: String, group: String?, kind: ContentKind = ContentKind.LIVE) =
        Channel(id = id, name = id, streamUrl = "http://x/$id", group = group, kind = kind)

    private val playlist = listOf(
        ch("rtp1", "NOTICIAS"),
        ch("dazn1", "DAZN"),
        ch("rtp2", "NOTICIAS"),
        ch("untagged", null),
        ch("blank", "  "),
        ch("film", "FILMES", ContentKind.MOVIE),
        ch("ep1", "SERIES", ContentKind.SERIES),
        ch("ep2", "SERIES", ContentKind.SERIES),
    )

    @Test
    fun `live channels are grouped by group-title in first-seen order`() {
        val categories = Library.liveCategories(playlist)

        assertEquals(listOf("NOTICIAS", "DAZN", "Other"), categories.map { it.name })
        assertEquals(listOf("rtp1", "rtp2"), categories[0].channels.map { it.id })
        assertEquals(listOf("dazn1"), categories[1].channels.map { it.id })
        assertEquals(listOf("untagged", "blank"), categories[2].channels.map { it.id })
    }

    @Test
    fun `movies and series are kept flat, in playlist order, and never appear as live categories`() {
        assertEquals(listOf("film"), Library.movies(playlist).map { it.id })
        assertEquals(listOf("ep1", "ep2"), Library.series(playlist).map { it.id })
        assertEquals(5, Library.liveCategories(playlist).sumOf { it.channels.size })
    }
}
