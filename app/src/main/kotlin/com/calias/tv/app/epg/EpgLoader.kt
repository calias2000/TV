package com.calias.tv.app.epg

import android.util.Xml
import com.calias.tv.app.httpGet
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.epg.Xmltv
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Downloads the XMLTV guide and parses it while it streams in. Keeps only the programs of
 * `wantedIds` from one hour ago to 24 hours ahead.
 */
object EpgLoader {

    // The guide is about 11 MB, bigger than the playlist, so it gets a longer time budget.
    private val TIMEOUT = 90.seconds

    suspend fun load(epgUrl: String, wantedIds: Set<String>, nowMs: Long): Guide =
        httpGet(epgUrl, "Guide", TIMEOUT) { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, null) // null: the parser reads the encoding from the XML header
            Xmltv.parse(parser, wantedIds, nowMs - 1.hours.inWholeMilliseconds, nowMs + 24.hours.inWholeMilliseconds)
        }
}
