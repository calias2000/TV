package com.calias.tv.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.core.Channel
import com.calias.tv.core.recents.Recents
import com.calias.tv.core.resume.Resume
import com.calias.tv.core.resume.ResumeRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// One DataStore instance per file, declared once at top level (DataStore docs).
private val Context.dataStore by preferencesDataStore(name = "tv")

private val DARK = booleanPreferencesKey("dark")
private val RESUME = stringPreferencesKey("resume")
private val RECENT_LIVE = stringPreferencesKey("recent_live")
private val FONT = stringPreferencesKey("font")
private val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")

/** The theme, font and subtitle size the app is drawn with. */
data class Look(val dark: Boolean, val font: AppFont, val subtitleSize: SubtitleSize)

/**
 * The small things the app remembers between runs: the theme, where each movie or series was
 * left, the recent live channels, the font and the subtitle size. Screens use this ViewModel; none of them touch DataStore directly. Writes run in
 * [viewModelScope], so leaving the player does not cancel a save.
 */
class PrefsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = app.dataStore

    /**
     * The theme, the font and the subtitle size. Null until DataStore has been read, so the first frame is
     * never drawn in the wrong theme or font.
     */
    val look: StateFlow<Look?> = store.data
        .map { prefs ->
            Look(
                dark = prefs[DARK] ?: true,
                font = AppFont.entries.firstOrNull { it.name == prefs[FONT] } ?: AppFont.DEFAULT,
                subtitleSize = SubtitleSize.entries.firstOrNull { it.name == prefs[SUBTITLE_SIZE] } ?: SubtitleSize.DEFAULT,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Everything remembered about where movies and episodes were left, for "Continue watching". */
    val resume: StateFlow<Map<String, ResumeRecord>> = store.data
        .map { Resume.decode(it[RESUME].orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Stream URLs of the recent live channels, newest first. */
    val recentLive: StateFlow<List<String>> = store.data
        .map { Recents.decode(it[RECENT_LIVE].orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFont(font: AppFont) {
        viewModelScope.launch { store.edit { it[FONT] = font.name } }
    }

    fun setSubtitleSize(size: SubtitleSize) {
        viewModelScope.launch { store.edit { it[SUBTITLE_SIZE] = size.name } }
    }

    fun toggleDark() {
        viewModelScope.launch { store.edit { it[DARK] = !(it[DARK] ?: true) } }
    }

    /** Remember [channel] as the most recent live channel. */
    fun saveRecentLive(channel: Channel) {
        viewModelScope.launch {
            store.edit { prefs ->
                val recents = Recents.decode(prefs[RECENT_LIVE].orEmpty())
                prefs[RECENT_LIVE] = Recents.encode(Recents.add(recents, channel.streamUrl))
            }
        }
    }

    /** Where to start [channel]: the saved position if this exact title was left earlier, else 0. */
    suspend fun startPosition(channel: Channel): Long {
        val key = Resume.key(channel) ?: return 0L
        val records = Resume.decode(store.data.first()[RESUME].orEmpty())
        return Resume.startPosition(records[key], channel.name)
    }

    /** Remember where the user is in [channel]. Does nothing for live channels. */
    fun saveProgress(channel: Channel, positionMs: Long, durationMs: Long) {
        val key = Resume.key(channel) ?: return
        viewModelScope.launch {
            store.edit { prefs ->
                val records = Resume.decode(prefs[RESUME].orEmpty())
                val updated = Resume.update(records, key, channel.name, positionMs, durationMs, System.currentTimeMillis())
                prefs[RESUME] = Resume.encode(updated)
            }
        }
    }
}
