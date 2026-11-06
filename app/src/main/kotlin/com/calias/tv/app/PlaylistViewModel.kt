package com.calias.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.core.playlist.Playlist
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
class PlaylistViewModel : ViewModel() {

    private val _state = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    private var syncJob: Job? = null

    init {
        sync()
    }

    /** Fetches again. Cancels a fetch that is still running. */
    fun sync() {
        syncJob?.cancel()
        _state.value = PlaylistState.Loading
        syncJob = viewModelScope.launch {
            _state.value = try {
                PlaylistState.Loaded(PlaylistLoader.load())
            } catch (e: Exception) {
                PlaylistState.Failed(e.message ?: e.toString())
            }
        }
    }
}
