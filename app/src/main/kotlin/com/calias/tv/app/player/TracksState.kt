package com.calias.tv.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import com.calias.tv.app.TrackPrefs
import java.util.Locale

/** The two lists the movie bar offers: the audio tracks and the subtitle tracks of the file. */
enum class TrackMenu(val label: String) { AUDIO("Audio"), SUBTITLES("Subtitles") }

/** One audio or subtitle track of the file, as the menu shows it. */
class TrackOption(
    internal val group: Tracks.Group,
    internal val index: Int,
    val label: String,
    val language: String?, // null when the file does not say
    val supported: Boolean,
    val selected: Boolean,
)

/**
 * The audio and subtitle tracks the current file offers, and the subtitle line showing now. Reads the
 * stable Media3 track API ([Player.getCurrentTracks], [Player.setTrackSelectionParameters]); Media3
 * 1.12 will bring ready-made holders for this, but it is only an alpha today. [onChoice] hears every pick
 * the user makes, as a language code (or [TrackPrefs.OFF] for subtitles), so it can be remembered. A plain
 * state holder.
 */
@Stable
class TracksState(private val player: Player, private val onChoice: (TrackMenu, String) -> Unit = { _, _ -> }) {
    var audio by mutableStateOf<List<TrackOption>>(emptyList())
        private set
    var text by mutableStateOf<List<TrackOption>>(emptyList())
        private set

    /** The subtitle line to draw now (empty when there is none). */
    var subtitle by mutableStateOf("")
        private set

    val subtitlesOn: Boolean get() = text.any { it.selected }

    internal fun update(tracks: Tracks) {
        audio = options(tracks, C.TRACK_TYPE_AUDIO)
        text = options(tracks, C.TRACK_TYPE_TEXT)
    }

    internal fun update(cues: CueGroup) {
        subtitle = cues.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
    }

    /**
     * Plays [option] (and turns its kind back on, in case subtitles were off). Its language becomes the
     * preferred one, so the next movie in this player starts with it too.
     */
    fun select(option: TrackOption) {
        val isAudio = option.group.type == C.TRACK_TYPE_AUDIO
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(option.group.type, false)
            .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
        option.language?.let {
            if (isAudio) builder.setPreferredAudioLanguage(it) else builder.setPreferredTextLanguage(it)
            onChoice(if (isAudio) TrackMenu.AUDIO else TrackMenu.SUBTITLES, it)
        }
        player.trackSelectionParameters = builder.build()
    }

    fun hideSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        subtitle = ""
        onChoice(TrackMenu.SUBTITLES, TrackPrefs.OFF)
    }
}

@Composable
fun rememberTracksState(player: Player, onChoice: (TrackMenu, String) -> Unit): TracksState {
    val latest by rememberUpdatedState(onChoice)
    val state = remember { TracksState(player) { menu, language -> latest(menu, language) } }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) = state.update(tracks)
            override fun onCues(cueGroup: CueGroup) = state.update(cueGroup)
        }
        state.update(player.currentTracks)
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return state
}

/** The tracks of [type] in [tracks], named by language. A language used twice gets a number ("Spanish", "Spanish 2"). */
internal fun options(tracks: Tracks, type: Int): List<TrackOption> {
    val seen = HashMap<String, Int>()
    return tracks.groups.filter { it.type == type }.flatMap { group ->
        (0 until group.length).map { index ->
            val format = group.getTrackFormat(index)
            val language = language(format)
            val count = (seen[language] ?: 0) + 1
            seen[language] = count
            val label = (if (count == 1) language else "$language $count") + surround(format, type)
            TrackOption(group, index, label, format.language?.takeIf { it != C.LANGUAGE_UNDETERMINED }, group.isTrackSupported(index), group.isTrackSelected(index))
        }
    }
}

private fun language(format: Format): String {
    val code = format.language?.takeIf { it != C.LANGUAGE_UNDETERMINED }
    return code?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH).ifBlank { it } }
        ?: format.label
        ?: "Unknown"
}

/** " · 5.1" or " · 7.1" for surround audio, else nothing. */
private fun surround(format: Format, type: Int): String =
    if (type != C.TRACK_TYPE_AUDIO) "" else when (format.channelCount) { 6 -> " · 5.1"; 8 -> " · 7.1"; else -> "" }

/**
 * These parameters with what the profile last chose: the audio language, and the subtitle language or no
 * subtitles. Whatever was never chosen is left as it is.
 */
fun TrackSelectionParameters.withPrefs(prefs: TrackPrefs): TrackSelectionParameters {
    val builder = buildUpon()
    prefs.audio?.let { builder.setPreferredAudioLanguage(it) }
    when (prefs.subtitles) {
        null -> {}
        TrackPrefs.OFF -> builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        else -> builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setPreferredTextLanguage(prefs.subtitles)
    }
    return builder.build()
}
