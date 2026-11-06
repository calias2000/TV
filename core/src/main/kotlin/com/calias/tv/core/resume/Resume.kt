package com.calias.tv.core.resume

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

/** Where a movie or episode was left: its title, the position, and when it was saved. */
data class ResumeRecord(val title: String, val positionMs: Long, val savedAt: Long)

/**
 * Remembers where the user stopped watching. One record per movie and one per series (the last
 * episode watched). Keyed by title, not by channel id, because ids are playlist positions and
 * change whenever the playlist changes. Pure logic, no Android.
 */
object Resume {

    const val MAX_RECORDS = 100

    /** Within this distance of the end, the item counts as finished and its record is dropped. */
    private const val FINISHED_MS = 30_000L

    /** Below this, resuming is pointless: start from the beginning. */
    private const val MIN_MS = 10_000L

    // "Show Name (2011) (US) - S01E01 - Pilot" -> "Show Name (2011) (US)"
    private val episodeMarker = Regex("""^(.+?) - S\d+E\d+\b""")

    /** The record key: the show name for episodes, the full title for movies. Null for live. */
    fun key(channel: Channel): String? = when (channel.kind) {
        ContentKind.LIVE -> null
        ContentKind.MOVIE -> "movie:${channel.name}"
        ContentKind.SERIES -> "series:" + (episodeMarker.find(channel.name)?.groupValues?.get(1) ?: channel.name)
    }

    /** The position to start from for [title], or 0 if the saved record is for another episode. */
    fun startPosition(record: ResumeRecord?, title: String): Long =
        if (record != null && record.title == title) record.positionMs else 0L

    /**
     * The records after leaving [title] at [positionMs]. A finished or barely started item removes
     * its record. The oldest records are dropped beyond [MAX_RECORDS].
     */
    fun update(
        records: Map<String, ResumeRecord>,
        key: String,
        title: String,
        positionMs: Long,
        durationMs: Long,
        now: Long,
    ): Map<String, ResumeRecord> {
        val finished = durationMs > 0 && positionMs >= durationMs - FINISHED_MS
        if (finished || positionMs < MIN_MS) return records - key
        val updated = records + (key to ResumeRecord(title, positionMs, now))
        return updated.entries.sortedByDescending { it.value.savedAt }.take(MAX_RECORDS)
            .associate { it.key to it.value }
    }

    /** One line per record: key, savedAt, position, title (tab separated; the title goes last). */
    fun encode(records: Map<String, ResumeRecord>): String =
        records.entries.joinToString("\n") { (k, r) -> "$k\t${r.savedAt}\t${r.positionMs}\t${r.title}" }

    /** Reads [encode] output. Lines that do not parse are skipped. */
    fun decode(text: String): Map<String, ResumeRecord> =
        text.lineSequence().mapNotNull { line ->
            val p = line.split('\t', limit = 4)
            val savedAt = p.getOrNull(1)?.toLongOrNull()
            val position = p.getOrNull(2)?.toLongOrNull()
            if (p.size == 4 && savedAt != null && position != null) p[0] to ResumeRecord(p[3], position, savedAt) else null
        }.toMap()
}
