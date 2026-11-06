package com.calias.tv.core.epg

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.xmlpull.v1.XmlPullParser

/**
 * Reads an XMLTV guide with a streaming pull parser, so a large file (about 11 MB for our
 * provider) is never held in memory. Only programs of the `wantedIds` that overlap the window
 * (`fromMs`, `toMs`) are kept, and only their titles, so the result is small.
 *
 * The caller creates the parser and sets its input (on Android: `Xml.newPullParser()`), which keeps
 * this module free of Android types.
 *
 * Times look like `20261106003000 +0100`. The offset is applied. A program with a time that does
 * not parse is skipped.
 */
object Xmltv {

    private val withOffset = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")
    private val withoutOffset = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parse(parser: XmlPullParser, wantedIds: Set<String>, fromMs: Long, toMs: Long): Guide {
        val found = HashMap<String, MutableList<Program>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                readProgram(parser, wantedIds, fromMs, toMs)?.let { (id, program) ->
                    found.getOrPut(id) { mutableListOf() }.add(program)
                }
            }
            event = parser.next()
        }
        return Guide(found.mapValues { (_, list) -> list.distinctBy { it.startMs }.sortedBy { it.startMs } })
    }

    /** Reads one `<programme>`, consuming up to its end tag. Null when it is not wanted. */
    private fun readProgram(
        parser: XmlPullParser,
        wantedIds: Set<String>,
        fromMs: Long,
        toMs: Long,
    ): Pair<String, Program>? {
        val channel = parser.getAttributeValue(null, "channel")
        val start = parser.getAttributeValue(null, "start")
        val stop = parser.getAttributeValue(null, "stop")
        val keep = channel != null && channel in wantedIds
        val startMs = if (keep) epochMs(start) else null
        val stopMs = if (keep) epochMs(stop) else null
        val inWindow = startMs != null && stopMs != null && stopMs > fromMs && startMs < toMs

        var title: String? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (inWindow && title == null && parser.name == "title") {
                        title = parser.nextText().trim() // moves to the title's end tag
                        depth--
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return null
            }
        }
        if (!inWindow || title.isNullOrEmpty()) return null
        return channel!! to Program(startMs, stopMs, title)
    }

    private fun epochMs(text: String?): Long? {
        val value = text?.trim() ?: return null
        return runCatching {
            if (value.length > 14) {
                OffsetDateTime.parse(value, withOffset).toInstant().toEpochMilli()
            } else {
                LocalDateTime.parse(value, withoutOffset).toInstant(ZoneOffset.UTC).toEpochMilli()
            }
        }.getOrNull()
    }
}
