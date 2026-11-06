package com.calias.tv.core.playlist

import com.calias.tv.core.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class M3uParserTest {

    @Test
    fun `reads header epg url, live channel and series entries`() {
        val text = """
            #EXTM3U url-tvg="https://example.com/epg.xml"
            #EXTINF:-1 tvg-id="rtp1.pt" tvg-type="live" group-title="NOTICIAS" tvg-logo="http://img.example/a.png?ver=1",RTP 1 HD
            http://example.com/live/1.ts
            #EXTINF:-1 tvg-type="series" group-title="SERIES",Show S01E01
            https://example.com/series/1.mkv
        """.trimIndent()

        val playlist = M3uParser.parse(text)

        assertEquals("https://example.com/epg.xml", playlist.epgUrl)
        assertEquals(2, playlist.channels.size)

        val live = playlist.channels[0]
        assertEquals("RTP 1 HD", live.name)
        assertEquals("NOTICIAS", live.group)
        assertEquals("http://img.example/a.png?ver=1", live.logo)
        assertEquals("rtp1.pt", live.tvgId)
        assertEquals(ContentKind.LIVE, live.kind)
        assertEquals("http://example.com/live/1.ts", live.streamUrl)

        val series = playlist.channels[1]
        assertEquals("Show S01E01", series.name)
        assertEquals(ContentKind.SERIES, series.kind)
        assertEquals("entry-1", series.id)
    }

    @Test
    fun `handles byte order mark and comma inside a quoted attribute`() {
        val text = "﻿#EXTM3U\n" +
            "#EXTINF:-1 tvg-name=\"A, B\" group-title=\"G\",Real Name\n" +
            "http://example.com/x.ts\n"

        val playlist = M3uParser.parse(text)

        assertNull(playlist.epgUrl)
        assertEquals(1, playlist.channels.size)
        assertEquals("Real Name", playlist.channels[0].name)
        assertEquals("G", playlist.channels[0].group)
    }
}
