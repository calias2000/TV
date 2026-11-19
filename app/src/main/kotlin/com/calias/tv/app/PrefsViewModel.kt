package com.calias.tv.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.player.TrackMenu
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.core.Channel
import com.calias.tv.core.resume.ResumeRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// One DataStore instance per file, declared once at top level (DataStore docs). A corrupt file is replaced
// by an empty one: the profiles are added again, which is cheaper than an app that cannot start.
internal val Context.dataStore by preferencesDataStore(name = "tv", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

/**
 * What the active profile remembers between runs, for the screens: the theme, font and subtitle size, the
 * audio and subtitle language it last chose, where each movie or series was left, and the recent live
 * channels. Screens use this ViewModel; none of them touch DataStore directly. Writes run in
 * [viewModelScope], so leaving the player does not cancel a save.
 */
class PrefsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProfileStore(app.dataStore)

    /** The look of the active profile. Null until DataStore has been read, so the first frame is never drawn in the wrong theme or font. */
    val look: StateFlow<Look?> = store.look.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val resume: StateFlow<Map<String, ResumeRecord>> = store.resume.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val recentLive: StateFlow<List<String>> = store.recentLive.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFont(font: AppFont) {
        viewModelScope.launch { store.setFont(font) }
    }

    fun setSubtitleSize(size: SubtitleSize) {
        viewModelScope.launch { store.setSubtitleSize(size) }
    }

    fun toggleDark() {
        viewModelScope.launch { store.toggleDark() }
    }

    fun saveRecentLive(channel: Channel) {
        viewModelScope.launch { store.saveRecentLive(channel) }
    }

    suspend fun trackPrefs(): TrackPrefs = store.trackPrefs.first()

    /** [language] is a language code, or [TrackPrefs.OFF] for subtitles. */
    fun saveTrackChoice(menu: TrackMenu, language: String) {
        viewModelScope.launch {
            when (menu) {
                TrackMenu.AUDIO -> store.setAudioLanguage(language)
                TrackMenu.SUBTITLES -> store.setSubtitleLanguage(language)
            }
        }
    }

    suspend fun startPosition(channel: Channel): Long = store.startPosition(channel)

    fun saveProgress(channel: Channel, positionMs: Long, durationMs: Long) {
        viewModelScope.launch { store.saveProgress(channel, positionMs, durationMs, System.currentTimeMillis()) }
    }
}
