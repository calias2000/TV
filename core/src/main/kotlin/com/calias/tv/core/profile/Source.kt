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

        // An access code is six digits, as on the server.
        private val CODE = Regex("[0-9]{6}")

        /** The access code in [typed] (spaces and dashes ignored), or null if it is not one. */
        fun code(typed: String): String? = typed.filterNot { it.isWhitespace() || it == '-' }.takeIf { CODE.matches(it) }

        private fun codeLink(serverBase: String, code: String) = "${serverBase.trimEnd('/')}/api/iptv/p/$code/list"

        /**
         * The source for what the user typed in the one field: an http(s) address (see [direct]), or an access
         * code, which becomes the playlist link of that code on the server at [serverBase]. Null if it is neither,
         * or if it is a code and this build has no server address (blank [serverBase]).
         */
        fun parse(typed: String, serverBase: String): Direct? =
            direct(typed) ?: code(typed)?.takeIf { serverBase.isNotBlank() }?.let { Direct(codeLink(serverBase, it)) }

        /** A [Direct] source for what the user typed, or null if it is not an http(s) URL with a host. */
        fun direct(typed: String): Direct? {
            val url = typed.trim()
            if (url.any(Char::isWhitespace)) return null
            val parsed = runCatching { URI(url) }.getOrNull() ?: return null
            return if (parsed.scheme?.lowercase() in setOf("http", "https") && !parsed.host.isNullOrEmpty()) Direct(url) else null
        }
    }
}
