package com.calias.tv.core.seek

/**
 * Seeking rules for movies and episodes.
 * Times are in milliseconds, the unit Media3 uses.
 */
object Seek {

    /**
     * How far one key event skips. Starts at 10 s. While the key is held (repeatCount grows) the
     * step grows to 30 s, then 60 s, so long skips need no extra presses.
     */
    fun stepMs(repeatCount: Int): Long = when {
        repeatCount < 5 -> 10_000L
        repeatCount < 15 -> 30_000L
        else -> 60_000L
    }

    /** Where to seek: [positionMs] + [deltaMs], never before 0 and never past [durationMs]. */
    fun target(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val moved = (positionMs + deltaMs).coerceAtLeast(0L)
        // Unknown duration (zero or negative): only the lower bound applies.
        return if (durationMs > 0) moved.coerceAtMost(durationMs) else moved
    }

    /** "m:ss" under one hour, "h:mm:ss" from one hour. Negative input shows as 0:00. */
    fun formatClock(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }
}
