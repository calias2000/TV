package com.calias.tv.core.recents

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

/**
 * The live channels played most recently, newest first, as stream URLs (stable, unlike list
 * positions).
 */
object Recents {

    /** How many recent live channels are remembered. */
    const val LIVE_LIMIT = 5

    /** [recents] after playing [url]: it goes first, an earlier entry of it is dropped, at most [limit] remain. */
    fun add(recents: List<String>, url: String, limit: Int = LIVE_LIMIT): List<String> =
        (listOf(url) + recents.filter { it != url }).take(limit)

    /** The live channels of [channels] for [urls], in the order of [urls]. URLs no longer in the playlist are left out. */
    fun resolve(urls: List<String>, channels: List<Channel>): List<Channel> {
        val live = channels.filter { it.kind == ContentKind.LIVE }
        return urls.mapNotNull { url -> live.firstOrNull { it.streamUrl == url } }
    }

    /** One URL per line. */
    fun encode(recents: List<String>): String = recents.joinToString("\n")

    /** Reads [encode] output. */
    fun decode(text: String): List<String> = text.lines().filter { it.isNotBlank() }
}
