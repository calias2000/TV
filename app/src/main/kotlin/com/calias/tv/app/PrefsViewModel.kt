package com.calias.tv.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.core.Channel
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
private val LAST_LIVE = stringPreferencesKey("last_live")

/**
 * The small things the app remembers between runs: the theme, where each movie or series was
 * left, and the last live channel. Screens use this ViewModel; none of them touch DataStore directly. Writes run in
 * [viewModelScope], so leaving the player does not cancel a save.
 */
class PrefsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = app.dataStore

    val dark: StateFlow<Boolean> = store.data
        .map { it[DARK] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Everything remembered about where movies and episodes were left, for "Continue watching". */
    val resume: StateFlow<Map<String, ResumeRecord>> = store.data
        .map { Resume.decode(it[RESUME].orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Stream URL of the last live channel that played (stable, unlike list positions), or null. */
    val lastLive: StateFlow<String?> = store.data
        .map { it[LAST_LIVE] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun toggleDark() {
        viewModelScope.launch { store.edit { it[DARK] = !dark.value } }
    }

    fun saveLastLive(channel: Channel) {
        viewModelScope.launch { store.edit { it[LAST_LIVE] = channel.streamUrl } }
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
