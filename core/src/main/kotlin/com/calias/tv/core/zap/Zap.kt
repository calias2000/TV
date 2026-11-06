package com.calias.tv.core.zap

/**
 * Channel zapping rules. Pure logic, no Android, so it is tested on the JVM.
 *
 * Convention (same as OwnTV): CH+ and D-pad up = next channel (+1), CH- and D-pad down = previous (-1).
 */
object Zap {

    /** Moves [delta] steps from [index] through a list of [size] items, wrapping around both ends. */
    fun step(index: Int, delta: Int, size: Int): Int {
        require(size > 0) { "size must be positive" }
        return ((index + delta) % size + size) % size
    }
}
