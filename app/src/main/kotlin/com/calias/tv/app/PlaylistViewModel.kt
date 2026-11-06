package com.calias.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.core.playlist.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the playlist screen shows: loading, loaded, or failed. */
sealed interface PlaylistState {
    data object Loading : PlaylistState
    data class Loaded(val playlist: Playlist) : PlaylistState
    data class Failed(val message: String) : PlaylistState
}

/**
 * Holds the playlist state for the screen. Starts the download once, and keeps the result
 * across configuration changes. The UI only reads [state].
 */
class PlaylistViewModel : ViewModel() {

    private val _state = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _state.value = PlaylistState.Loading
        viewModelScope.launch {
            _state.value = try {
                PlaylistState.Loaded(PlaylistLoader.load())
            } catch (e: Exception) {
                PlaylistState.Failed(e.message ?: e.toString())
            }
        }
    }
}
