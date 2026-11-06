package com.calias.tv.core.epg

/** One program of the TV guide. Times are epoch milliseconds, the unit of System.currentTimeMillis. */
data class Program(val startMs: Long, val stopMs: Long, val title: String)

/** What is on now and what follows. Either can be missing (gap in the guide). */
data class NowNext(val now: Program?, val next: Program?)

/**
 * The TV guide: programs per EPG channel id (the playlist's `tvg-id`). Several playlist entries
 * can share one id (for example the HD and 4K variants of a channel): each entry is still its own
 * row, they just look up the same guide data. Pure logic, no Android.
 */
class Guide(private val byChannel: Map<String, List<Program>>) {

    /** Programs of [tvgId] sorted by start, or null if the id is blank or unknown. */
    fun nowNext(tvgId: String?, nowMs: Long): NowNext? {
        val list = byChannel[tvgId ?: return null] ?: return null
        val now = list.firstOrNull { nowMs >= it.startMs && nowMs < it.stopMs }
        val next = list.firstOrNull { it.startMs >= (now?.stopMs ?: nowMs) }
        return if (now == null && next == null) null else NowNext(now, next)
    }

    companion object {
        val EMPTY = Guide(emptyMap())
    }
}
