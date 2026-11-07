package com.calias.tv.core.series

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EpisodesTest {

    private fun ep(name: String, kind: ContentKind = ContentKind.SERIES) = Channel("x", name, "http://u", null, kind = kind)

    private val list = listOf(
        ep("NF - Lupin - S01E01 - A"), // 0
        ep("NF - Lupin - S01E01 - A"), // 1 duplicate entry of the same episode
        ep("NF - Lupin - S01E02 - B"), // 2
        ep("D+ - Homeland - S01E01 - Pilot"), // 3 other show
        ep("RO - Some Movie", ContentKind.MOVIE), // 4
    )

    @Test
    fun `next skips duplicate entries of the same episode`() {
        assertEquals(2, Episodes.nextIndex(list, 0))
        assertEquals(2, Episodes.nextIndex(list, 1))
    }

    @Test
    fun `no next after the last episode of a show, for movies, or for a bad index`() {
        assertNull(Episodes.nextIndex(list, 2))
        assertNull(Episodes.nextIndex(list, 3))
        assertNull(Episodes.nextIndex(list, 4))
        assertNull(Episodes.nextIndex(list, 99))
    }
}
