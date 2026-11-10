package com.calias.tv.core.profile

import java.net.URI

/**
 * Where a profile gets its playlist. A sealed type, so a new kind of source (a linked backend account)
 * is one more case and the compiler shows every place that must handle it.
 */
sealed interface Source {

    /** An M3U URL kept on the TV; the TV downloads it itself. It can hold the provider's username and password, so screens never show it. */
    data class Direct(val url: String) : Source

    companion object {
        private const val DIRECT = "direct:"

        /** One line of text, for storing. */
        fun encode(source: Source): String = when (source) {
            is Direct -> DIRECT + source.url
        }

        /** Reads [encode] output. Null for text no source can be read from. */
        fun decode(text: String): Source? = when {
            text.startsWith(DIRECT) -> direct(text.removePrefix(DIRECT))
            else -> null
        }

        /** A [Direct] source for what the user typed, or null if it is not an http(s) URL with a host. */
        fun direct(typed: String): Direct? {
            val url = typed.trim()
            if (url.any(Char::isWhitespace)) return null
            val parsed = runCatching { URI(url) }.getOrNull() ?: return null
            return if (parsed.scheme?.lowercase() in setOf("http", "https") && !parsed.host.isNullOrEmpty()) Direct(url) else null
        }
    }
}
