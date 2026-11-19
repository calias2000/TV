package com.calias.tv.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.core.Channel
import com.calias.tv.core.profile.Profile
import com.calias.tv.core.profile.Profiles
import com.calias.tv.core.profile.Source
import com.calias.tv.core.recents.Recents
import com.calias.tv.core.resume.Resume
import com.calias.tv.core.resume.ResumeRecord
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val PROFILES = stringPreferencesKey("profiles")
private val ACTIVE = stringPreferencesKey("active")

/**
 * The keys of one profile's own data. They are the profile's id plus a name, in the one DataStore file
 * the app has: DataStore allows a single instance per file, and removing a profile is then one atomic edit.
 */
private class ProfileKeys(id: String) {
    val dark = booleanPreferencesKey("$id.dark")
    val resume = stringPreferencesKey("$id.resume")
    val recentLive = stringPreferencesKey("$id.recent_live")
    val font = stringPreferencesKey("$id.font")
    val subtitleSize = stringPreferencesKey("$id.subtitle_size")
    val audioLanguage = stringPreferencesKey("$id.audio_language")
    val subtitleLanguage = stringPreferencesKey("$id.subtitle_language")
    val all: List<Preferences.Key<*>> = listOf(dark, resume, recentLive, font, subtitleSize, audioLanguage, subtitleLanguage)
}

/** The theme, font and subtitle size the app is drawn with. */
data class Look(val dark: Boolean, val font: AppFont, val subtitleSize: SubtitleSize) {
    companion object {
        val DEFAULT = Look(dark = true, font = AppFont.DEFAULT, subtitleSize = SubtitleSize.DEFAULT)
    }
}

/** The profiles on this TV and which one is in use. [active] is null when none is chosen (the user picks one). */
data class ProfilesState(val profiles: List<Profile>, val activeId: String?) {
    val active: Profile? get() = profiles.firstOrNull { it.id == activeId }
}

/**
 * Everything the app remembers, in one DataStore: the list of profiles, the active one, and for each
 * profile its theme, font, subtitle size, the audio and subtitle language it last chose, where each movie
 * or series was left, and its recent live channels. The reads and writes of viewing state always go to the
 * active profile. No Android types, so it is tested on the JVM with a DataStore on a temporary file.
 */
class ProfileStore(private val store: DataStore<Preferences>) {

    val profiles: Flow<ProfilesState> = store.data.map { ProfilesState(it.profiles(), it[ACTIVE]) }

    val look: Flow<Look> = store.data.map { prefs ->
        val keys = prefs.activeKeys() ?: return@map Look.DEFAULT
        Look(
            dark = prefs[keys.dark] ?: Look.DEFAULT.dark,
            font = AppFont.entries.firstOrNull { it.name == prefs[keys.font] } ?: Look.DEFAULT.font,
            subtitleSize = SubtitleSize.entries.firstOrNull { it.name == prefs[keys.subtitleSize] } ?: Look.DEFAULT.subtitleSize,
        )
    }

    /** The audio and subtitle language the active profile last chose. */
    val trackPrefs: Flow<TrackPrefs> = store.data.map { prefs ->
        val keys = prefs.activeKeys() ?: return@map TrackPrefs()
        TrackPrefs(audio = prefs[keys.audioLanguage], subtitles = prefs[keys.subtitleLanguage])
    }

    /** Everything remembered about where movies and episodes were left, for "Continue watching". */
    val resume: Flow<Map<String, ResumeRecord>> = store.data.map { prefs ->
        Resume.decode(prefs.activeKeys()?.let { prefs[it.resume] }.orEmpty())
    }

    /** Stream URLs of the recent live channels, newest first. */
    val recentLive: Flow<List<String>> = store.data.map { prefs ->
        Recents.decode(prefs.activeKeys()?.let { prefs[it.recentLive] }.orEmpty())
    }

    /** Adds a profile and makes it the active one. Fails if the list is full. */
    suspend fun add(name: String, source: Source, id: String = UUID.randomUUID().toString()) {
        store.edit {
            it[PROFILES] = Profiles.encode(Profiles.add(it.profiles(), Profile(id, name, source)))
            it[ACTIVE] = id
        }
    }

    /** Changes the name and source of profile [id]; its viewing state stays. */
    suspend fun update(id: String, name: String, source: Source) {
        store.edit { it[PROFILES] = Profiles.encode(Profiles.replace(it.profiles(), Profile(id, name, source))) }
    }

    /** Removes profile [id] with all its viewing state. If it was the active one, none is active afterwards. */
    suspend fun remove(id: String) {
        store.edit { prefs ->
            prefs[PROFILES] = Profiles.encode(Profiles.remove(prefs.profiles(), id))
            ProfileKeys(id).all.forEach { prefs.remove(it) }
            if (prefs[ACTIVE] == id) prefs.remove(ACTIVE)
        }
    }

    /** Makes profile [id] the active one. Does nothing for an id that is not in the list. */
    suspend fun switchTo(id: String) {
        store.edit { prefs -> if (prefs.profiles().any { it.id == id }) prefs[ACTIVE] = id }
    }

    suspend fun setFont(font: AppFont) = editActive { prefs, keys -> prefs[keys.font] = font.name }

    suspend fun setSubtitleSize(size: SubtitleSize) = editActive { prefs, keys -> prefs[keys.subtitleSize] = size.name }

    suspend fun setAudioLanguage(language: String) = editActive { prefs, keys -> prefs[keys.audioLanguage] = language }

    /** [language] is a language code, or [TrackPrefs.OFF]. */
    suspend fun setSubtitleLanguage(language: String) = editActive { prefs, keys -> prefs[keys.subtitleLanguage] = language }

    suspend fun toggleDark() = editActive { prefs, keys -> prefs[keys.dark] = !(prefs[keys.dark] ?: Look.DEFAULT.dark) }

    /** Remembers [channel] as the most recent live channel. */
    suspend fun saveRecentLive(channel: Channel) = editActive { prefs, keys ->
        val recents = Recents.decode(prefs[keys.recentLive].orEmpty())
        prefs[keys.recentLive] = Recents.encode(Recents.add(recents, channel.streamUrl))
    }

    /** Where to start [channel]: the saved position if this exact title was left earlier, else 0. */
    suspend fun startPosition(channel: Channel): Long {
        val key = Resume.key(channel) ?: return 0L
        val prefs = store.data.first()
        val records = Resume.decode(prefs.activeKeys()?.let { prefs[it.resume] }.orEmpty())
        return Resume.startPosition(records[key], channel.name)
    }

    /** Remembers where the user is in [channel]. Does nothing for live channels. */
    suspend fun saveProgress(channel: Channel, positionMs: Long, durationMs: Long, nowMs: Long) {
        val key = Resume.key(channel) ?: return
        editActive { prefs, keys ->
            val records = Resume.decode(prefs[keys.resume].orEmpty())
            prefs[keys.resume] = Resume.encode(Resume.update(records, key, channel.name, positionMs, durationMs, nowMs))
        }
    }

    /** Edits the active profile's data. The profile is looked up inside the edit, so a switch cannot send a write to the wrong one. */
    private suspend fun editActive(block: (MutablePreferences, ProfileKeys) -> Unit) {
        store.edit { prefs -> prefs.activeKeys()?.let { block(prefs, it) } }
    }

    private fun Preferences.profiles() = Profiles.decode(this[PROFILES].orEmpty())

    /** The keys of the active profile; null if none is active or the active id is no longer in the list. */
    private fun Preferences.activeKeys(): ProfileKeys? {
        val id = this[ACTIVE] ?: return null
        return if (profiles().any { it.id == id }) ProfileKeys(id) else null
    }
}
