package com.calias.tv.core.color

import kotlin.math.max
import kotlin.math.min

/**
 * Finds the main color of a small picture, for the tinted backdrop. Grays, near-black and near-white
 * pixels do not count (a poster's dark frame is not its color); the rest are averaged, with the more
 * saturated and brighter pixels counting more.
 */
object Dominant {

    private const val MIN_ALPHA = 128
    private const val MIN_BRIGHTNESS = 0.2f
    private const val MIN_SATURATION = 0.25f

    /** The dominant opaque color (ARGB) of [pixels] (ARGB ints), or null if none of them has a clear color. */
    fun color(pixels: IntArray): Int? {
        var weightSum = 0.0
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        for (pixel in pixels) {
            if ((pixel ushr 24) < MIN_ALPHA) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val top = max(r, max(g, b))
            val bottom = min(r, min(g, b))
            val brightness = top / 255f
            val saturation = if (top == 0) 0f else (top - bottom).toFloat() / top
            if (brightness < MIN_BRIGHTNESS || saturation < MIN_SATURATION) continue
            val weight = (saturation * brightness).toDouble()
            weightSum += weight
            red += r * weight
            green += g * weight
            blue += b * weight
        }
        if (weightSum == 0.0) return null
        return (0xFF shl 24) or
            ((red / weightSum).toInt() shl 16) or
            ((green / weightSum).toInt() shl 8) or
            (blue / weightSum).toInt()
    }
}
