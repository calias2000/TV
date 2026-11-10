package com.calias.tv.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.core.profile.Profile
import com.calias.tv.core.profile.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The profiles on this TV, for the screens that list, add, edit, delete and switch them. */
class ProfilesViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProfileStore(app.dataStore)

    /** Null until DataStore has been read. */
    val state: StateFlow<ProfilesState?> = store.profiles.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun switchTo(id: String) {
        viewModelScope.launch { store.switchTo(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.remove(id) }
    }

    /**
     * Saves a new profile ([existing] null) or the changes to [existing]. A new or changed source must first
     * load a playlist with channels, so a profile never starts out broken. Returns why it failed, or null
     * when saved; nothing is saved on a failure.
     */
    suspend fun save(existing: Profile?, name: String, source: Source): String? {
        if (existing?.source != source) {
            try {
                check(PlaylistLoader.load(source).channels.isNotEmpty()) { "The playlist has no channels" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return e.message ?: e.toString()
            }
        }
        if (existing == null) store.add(name, source) else store.update(existing.id, name, source)
        return null
    }
}
