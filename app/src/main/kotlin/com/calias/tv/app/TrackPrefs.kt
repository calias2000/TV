package com.calias.tv.app

/**
 * What a profile last chose for movies and episodes: the language of the audio, and of the subtitles (or
 * [OFF]). Null means never chosen, so the file's own default plays. The next movie starts the same way.
 */
data class TrackPrefs(val audio: String? = null, val subtitles: String? = null) {
    companion object {
        const val OFF = "off"
    }
}
