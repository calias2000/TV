package com.calias.tv.core.video

/**
 * The common name of a picture size. Names in a playlist ("4K", "HD") are the provider's claim and often wrong, so
 * this names the size the stream really has. The width counts as much as the height: a film frame of 1920x800
 * is still Full HD.
 */
object Quality {
    /** The name for a [width] by [height] picture, or null while the size is not known (0 by 0). */
    fun label(width: Int, height: Int): String? = when {
        width <= 0 || height <= 0 -> null
        width >= 3200 || height >= 2000 -> "4K"
        width >= 2400 || height >= 1300 -> "QHD"
        width >= 1800 || height >= 1000 -> "Full HD"
        width >= 1200 || height >= 700 -> "HD"
        else -> "SD"
    }
}
