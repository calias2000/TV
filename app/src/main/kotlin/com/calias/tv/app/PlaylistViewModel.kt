package com.calias.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.app.epg.EpgLoader
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.playlist.Playlist
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** The playlist as the UI sees it: downloading, available, or unavailable with a reason. */
sealed interface PlaylistState {
    data object Loading : PlaylistState
    data class Loaded(val playlist: Playlist) : PlaylistState
    data class Failed(val message: String) : PlaylistState
}

/**
 * Fetches the playlist from its source. There is no local cache: the source is the truth, and a
 * failed fetch is reported as [PlaylistState.Failed] rather than hidden behind old data.
 *
 * The TV guide loads in the background after the playlist. It is optional: until it arrives, or if it
 * fails, [guide] is empty and screens show channel names only.
 */
class PlaylistViewModel : ViewModel() {

    private val _state = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    private val _guide = MutableStateFlow(Guide.EMPTY)
    val guide: StateFlow<Guide> = _guide.asStateFlow()

    private var syncJob: Job? = null

    init {
        sync()
    }

    private suspend fun loadGuide(playlist: Playlist) {
        val url = playlist.epgUrl ?: return
        val ids = playlist.channels.mapNotNullTo(HashSet()) { it.tvgId?.takeIf(String::isNotBlank) }
        // A missing guide is not an error for the user: rows simply show names.
        _guide.value = try {
            EpgLoader.load(url, ids, System.currentTimeMillis())
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive() // a canceled sync is not a guide failure
            Guide.EMPTY
        }
    }

    /** Fetches again. Cancels a fetch that is still running. */
    fun sync() {
        syncJob?.cancel()
        _state.value = PlaylistState.Loading
        _guide.value = Guide.EMPTY
        syncJob = viewModelScope.launch {
            val playlist = try {
                PlaylistLoader.load()
            } catch (e: Exception) {
                ensureActive() // a canceled sync is not a failed playlist
                _state.value = PlaylistState.Failed(e.message ?: e.toString())
                return@launch
            }
            _state.value = PlaylistState.Loaded(playlist)
            loadGuide(playlist)
        }
    }
}
