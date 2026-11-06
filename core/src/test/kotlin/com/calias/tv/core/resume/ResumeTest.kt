package com.calias.tv.core.resume

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `encode and decode round trip, bad lines skipped`() {
        val map = mapOf("series:A - B" to ResumeRecord("A - B - S01E01 - X", 61_000, 5))
        assertEquals(map, Resume.decode(Resume.encode(map)))
        assertEquals(emptyMap(), Resume.decode("garbage\n\n"))
    }
}
