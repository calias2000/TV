package com.calias.tv.core.series

import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind
import com.calias.tv.core.resume.Resume

/** Episode order inside the flat series list. */
object Episodes {

    /**
     * The index of the episode that follows [index], or null when there is none.
     *
     * The playlist has no season data, so "next" means: the next entry of the same show (same
     * show name, see [Resume.key]) with a different title. Entries repeated with the same title
     * are skipped. The list is expected in playlist order, which lists a show's episodes together.
     * Movies and live channels have no next episode.
     */
    fun nextIndex(channels: List<Channel>, index: Int): Int? {
        val current = channels.getOrNull(index) ?: return null
        if (current.kind != ContentKind.SERIES) return null
        val show = Resume.key(current)
        for (i in index + 1 until channels.size) {
            val candidate = channels[i]
            if (Resume.key(candidate) != show) return null
            if (candidate.name != current.name) return i
        }
        return null
    }
}
