package com.calias.tv.core.epg

/** One program of the TV guide. Times are epoch milliseconds, the unit of System.currentTimeMillis. */
data class Program(val startMs: Long, val stopMs: Long, val title: String) {

    /** How far along the program is at [nowMs], from 0 (not started) to 1 (over). */
    fun progress(nowMs: Long): Float =
        ((nowMs - startMs).toFloat() / (stopMs - startMs)).coerceIn(0f, 1f)
}

/** What is on now and what follows. Either can be missing (gap in the guide). */
data class NowNext(val now: Program?, val next: Program?)

/**
 * The TV guide: programs per EPG channel id (the playlist's `tvg-id`). Several playlist entries
 * can share one id (for example the HD and 4K variants of a channel): each entry is still its own
 * row, they just look up the same guide data.
 */
class Guide(private val byChannel: Map<String, List<Program>>) {

    /**
     * The programs of [tvgId] that have not ended at [nowMs], in time order: the one on now (if
     * any) first, then the ones that follow. Empty if the id is blank or unknown.
     */
    fun schedule(tvgId: String?, nowMs: Long): List<Program> =
        byChannel[tvgId ?: return emptyList()]?.filter { it.stopMs > nowMs }.orEmpty()

    /** What is on now and next for [tvgId] at [nowMs], or null if there is nothing near [nowMs]. */
    fun nowNext(tvgId: String?, nowMs: Long): NowNext? {
        val upcoming = schedule(tvgId, nowMs)
        val now = upcoming.firstOrNull()?.takeIf { it.startMs <= nowMs }
        val next = upcoming.firstOrNull { it.startMs > nowMs }
        return if (now == null && next == null) null else NowNext(now, next)
    }

    companion object {
        val EMPTY = Guide(emptyMap())
    }
}
