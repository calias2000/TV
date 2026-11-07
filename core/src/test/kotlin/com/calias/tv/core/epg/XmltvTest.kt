package com.calias.tv.core.epg

import java.io.StringReader
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.kxml2.io.KXmlParser

class XmltvTest {

    private fun ms(iso: String) = OffsetDateTime.parse(iso).toInstant().toEpochMilli()

    private val xml = """
        <?xml version="1.0" encoding="utf-8" ?>
        <tv>
          <channel id="a.pt"><display-name>A</display-name></channel>
          <programme start="20261106120000 +0100" stop="20261106130000 +0100" channel="a.pt"><title>Noticias &amp; Mais</title><desc>x</desc></programme>
          <programme start="20261106130000 +0100" stop="20261106140000 +0100" channel="a.pt"><title>Filme</title></programme>
          <programme start="20261106120000 +0000" stop="20261106130000 +0000" channel="other.pt"><title>Ignored</title></programme>
          <programme start="20261101000000 +0000" stop="20261101010000 +0000" channel="a.pt"><title>Too old</title></programme>
          <programme start="garbage" stop="20261106150000 +0100" channel="a.pt"><title>Bad time</title></programme>
        </tv>
    """.trimIndent()

    private fun guide(): Guide {
        val parser = KXmlParser().apply { setInput(StringReader(xml)) }
        return Xmltv.parse(
            parser,
            wantedIds = setOf("a.pt"),
            fromMs = ms("2026-11-06T10:00:00Z"),
            toMs = ms("2026-11-07T10:00:00Z"),
        )
    }

    @Test
    fun `keeps wanted programs in the window, applies the offset, decodes entities`() {
        // 12:30 at +01:00 is 11:30 UTC.
        val nn = guide().nowNext("a.pt", ms("2026-11-06T11:30:00Z"))
        assertEquals("Noticias & Mais", nn?.now?.title)
        assertEquals("Filme", nn?.next?.title)
        assertEquals(ms("2026-11-06T11:00:00Z"), nn?.now?.startMs)
    }

    @Test
    fun `next is shown in a gap, nothing after the last program, unknown ids give null`() {
        val g = guide()
        val before = g.nowNext("a.pt", ms("2026-11-06T10:30:00Z"))
        assertNull(before?.now)
        assertEquals("Noticias & Mais", before?.next?.title)
        assertNull(g.nowNext("a.pt", ms("2026-11-06T20:00:00Z")))
        assertNull(g.nowNext("other.pt", ms("2026-11-06T11:30:00Z")))
        assertNull(g.nowNext(null, 0))
    }

    @Test
    fun `schedule lists the running program first, then the following ones`() {
        val g = guide()
        assertEquals(listOf("Noticias & Mais", "Filme"), g.schedule("a.pt", ms("2026-11-06T11:30:00Z")).map { it.title })
        assertEquals(listOf("Filme"), g.schedule("a.pt", ms("2026-11-06T12:30:00Z")).map { it.title })
        assertEquals(emptyList(), g.schedule("a.pt", ms("2026-11-06T20:00:00Z")))
        assertEquals(emptyList(), g.schedule(null, 0))
    }
}
