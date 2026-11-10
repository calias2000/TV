package com.calias.tv.core.resume

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import com.calias.tv.core.series.Shows

/** Where a movie or episode was left: its title, the position, when it was saved, and its length (0 if unknown). */
data class ResumeRecord(val title: String, val positionMs: Long, val savedAt: Long, val durationMs: Long = 0)

/** A movie or episode the user can continue, with where they stopped. */
data class ContinueItem(val channel: Channel, val positionMs: Long)

/**
 * Remembers where the user stopped watching. One record per movie and one per series (the last
 * episode watched). Keyed by title, not by channel id, because ids are playlist positions and
 * change whenever the playlist changes.
 */
object Resume {

    const val MAX_RECORDS = 100

    /** Within this distance of the end, the item counts as finished and its record is dropped. */
    private const val FINISHED_MS = 30_000L

    /** Below this, resuming is pointless: start from the beginning. */
    private const val MIN_MS = 10_000L

    /** The record key: the show name for episodes, the full title for movies. Null for live. */
    fun key(channel: Channel): String? = when (channel.kind) {
        ContentKind.LIVE -> null
        ContentKind.MOVIE -> "movie:${channel.name}"
        ContentKind.SERIES -> "series:" + (Shows.showOf(channel.name) ?: channel.name)
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
        val updated = records + (key to ResumeRecord(title, positionMs, now, durationMs))
        return updated.entries.sortedByDescending { it.value.savedAt }.take(MAX_RECORDS)
            .associate { it.key to it.value }
    }

    /** How far [channel] was left, from 0 to 1, or null if this exact title has no record (or its length is unknown). */
    fun progress(records: Map<String, ResumeRecord>, channel: Channel): Float? {
        val record = records[key(channel)]?.takeIf { it.title == channel.name && it.durationMs > 0 }
        return record?.let { (it.positionMs.toFloat() / it.durationMs).coerceIn(0f, 1f) }
    }

    /**
     * What to offer under "Continue watching": the most recently watched records, newest first,
     * matched to the playlist by title. Records whose title is no longer in the playlist are left out.
     */
    fun continueWatching(records: Map<String, ResumeRecord>, channels: List<Channel>, limit: Int = 10): List<ContinueItem> {
        val byTitle = channels.filter { it.kind != ContentKind.LIVE }.distinctBy { it.name }.associateBy { it.name }
        return records.values
            .sortedByDescending { it.savedAt }
            .mapNotNull { record -> byTitle[record.title]?.let { ContinueItem(it, record.positionMs) } }
            .take(limit)
    }

    /** One line per record: key, savedAt, position, duration, title (tab separated; the title goes last). */
    fun encode(records: Map<String, ResumeRecord>): String =
        records.entries.joinToString("\n") { (k, r) -> "$k\t${r.savedAt}\t${r.positionMs}\t${r.durationMs}\t${r.title}" }

    /** Reads [encode] output. Lines that do not parse are skipped. */
    fun decode(text: String): Map<String, ResumeRecord> =
        text.lineSequence().mapNotNull { line ->
            val p = line.split('\t', limit = 5)
            val savedAt = p.getOrNull(1)?.toLongOrNull()
            val position = p.getOrNull(2)?.toLongOrNull()
            val duration = p.getOrNull(3)?.toLongOrNull()
            if (p.size == 5 && savedAt != null && position != null && duration != null) {
                p[0] to ResumeRecord(p[4], position, savedAt, duration)
            } else {
                null
            }
        }.toMap()
}
