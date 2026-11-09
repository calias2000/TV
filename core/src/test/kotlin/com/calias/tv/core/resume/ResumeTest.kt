package com.calias.tv.core.resume

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResumeTest {

    private fun ch(name: String, kind: ContentKind) = Channel("x", name, "http://u", null, kind = kind)

    @Test
    fun `episodes of one show share a key, movies use the full title, live has none`() {
        val e1 = ch("D+ - Homeland (2011) (US) - S01E01 - Pilot", ContentKind.SERIES)
        val e2 = ch("D+ - Homeland (2011) (US) - S02E05 - Other", ContentKind.SERIES)
        assertEquals("series:D+ - Homeland (2011) (US)", Resume.key(e1))
        assertEquals("series:D+ - Homeland (2011) (US)", Resume.key(e2))
        assertEquals("movie:RO - Paris Can Wait", Resume.key(ch("RO - Paris Can Wait", ContentKind.MOVIE)))
        assertNull(Resume.key(ch("RTP 1", ContentKind.LIVE)))
    }

    @Test
    fun `resume only for the same title`() {
        val r = ResumeRecord("ep1", 90_000, 1)
        assertEquals(90_000L, Resume.startPosition(r, "ep1"))
        assertEquals(0L, Resume.startPosition(r, "ep2"))
        assertEquals(0L, Resume.startPosition(null, "ep1"))
    }

    @Test
    fun `finished or barely started items drop their record`() {
        val start = Resume.update(emptyMap(), "k", "t", 60_000, 600_000, now = 1)
        assertEquals(60_000L, start["k"]?.positionMs)
        assertEquals(emptyMap(), Resume.update(start, "k", "t", 580_000, 600_000, now = 2))
        assertEquals(emptyMap(), Resume.update(start, "k", "t", 5_000, 600_000, now = 2))
    }

    @Test
    fun `oldest records are dropped beyond the cap`() {
        var map = emptyMap<String, ResumeRecord>()
        for (i in 1..Resume.MAX_RECORDS + 1) map = Resume.update(map, "k$i", "t", 60_000, 600_000, now = i.toLong())
        assertEquals(Resume.MAX_RECORDS, map.size)
        assertNull(map["k1"])
    }

    @Test
    fun `continue watching lists newest first, only titles still in the playlist, never live`() {
        val old = ch("Old Movie", ContentKind.MOVIE)
        val new = ch("New - S01E02 - X", ContentKind.SERIES)
        val records = mapOf(
            "movie:Old Movie" to ResumeRecord("Old Movie", 60_000, 1),
            "series:New" to ResumeRecord("New - S01E02 - X", 90_000, 2),
            "movie:Gone" to ResumeRecord("Gone", 60_000, 3),
        )
        val items = Resume.continueWatching(records, listOf(old, new, ch("Old Movie", ContentKind.LIVE)))
        assertEquals(listOf(new, old), items.map { it.channel })
        assertEquals(90_000L, items.first().positionMs)
        assertEquals(1, Resume.continueWatching(records, listOf(old, new), limit = 1).size)
    }

    @Test
    fun `encode and decode round trip, bad lines skipped`() {
        val map = mapOf("series:A - B" to ResumeRecord("A - B - S01E01 - X", 61_000, 5, 600_000))
        assertEquals(map, Resume.decode(Resume.encode(map)))
        assertTrue(Resume.decode("garbage\n\n").isEmpty())
    }

    @Test
    fun `progress is the saved fraction, only for the same title with a known length`() {
        val movie = ch("M", ContentKind.MOVIE)
        val records = Resume.update(emptyMap(), "movie:M", "M", 150_000, 600_000, now = 1)
        assertEquals(0.25f, Resume.progress(records, movie))
        assertNull(Resume.progress(records, ch("Other", ContentKind.MOVIE)))
        assertNull(Resume.progress(mapOf("movie:M" to ResumeRecord("M", 60_000, 1)), movie)) // length unknown
        assertNull(Resume.progress(records, ch("M", ContentKind.LIVE)))
    }
}
