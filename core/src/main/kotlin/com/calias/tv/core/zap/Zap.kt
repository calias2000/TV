package com.calias.tv.core.zap

/**
 * Channel zapping rules.
 *
 * CH+ = next channel (+1), CH- = previous (-1).
 */
object Zap {

    /** Moves [delta] steps from [index] through a list of [size] items, wrapping around both ends. */
    fun step(index: Int, delta: Int, size: Int): Int {
        require(size > 0) { "size must be positive" }
        return ((index + delta) % size + size) % size
    }
}
