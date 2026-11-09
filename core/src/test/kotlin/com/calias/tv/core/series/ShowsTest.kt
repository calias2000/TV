package com.calias.tv.core.series

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShowsTest {

    private fun ep(name: String, logo: String? = null) =
        Channel("x", name, "http://u", null, logo = logo, kind = ContentKind.SERIES)

    @Test
    fun `reads the show and the episode from a title`() {
        val title = "D+ - Homeland (2011) (US) - S01E01 - Pilot"
        assertEquals("D+ - Homeland (2011) (US)", Shows.showOf(title))
        assertEquals("S01E01 - Pilot", Shows.episodeOf(title))
        assertNull(Shows.showOf("Just A Title"))
        assertEquals("Just A Title", Shows.episodeOf("Just A Title"))
    }

    @Test
    fun `groups episodes by show in first-seen order, duplicates kept`() {
        val shows = Shows.group(
            listOf(ep("A - S01E01 - x"), ep("B - S01E01 - y"), ep("A - S01E01 - x"), ep("A - S01E02 - z"), ep("Odd")),
        )
        assertEquals(listOf("A", "B", "Odd"), shows.map { it.name })
        assertEquals(listOf(3, 1, 1), shows.map { it.episodes.size })
    }

    @Test
    fun `a show's picture is the first episode that has one`() {
        assertEquals("p", Show("A", listOf(ep("a", null), ep("b", " "), ep("c", "p"))).logo)
        assertNull(Show("A", listOf(ep("a"))).logo)
    }
}
