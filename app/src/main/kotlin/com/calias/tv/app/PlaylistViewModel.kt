package com.calias.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.core.epg.Guide
import com.calias.tv.core.playlist.Playlist
import com.calias.tv.core.profile.Source
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** The playlist as the UI sees it: downloading, available, or unavailable with a reason. */
sealed interface PlaylistState {
    data object Loading : PlaylistState
    data class Loaded(val playlist: Playlist) : PlaylistState
    data class Failed(val message: String) : PlaylistState
}

/** What the playlist state says, for Settings and for Home's status pill. Null while loading: a spinner shows then. */
internal fun statusText(state: PlaylistState): String? = when (state) {
    PlaylistState.Loading -> null
    is PlaylistState.Loaded -> "Playlist loaded: ${state.playlist.channels.size} entries"
    is PlaylistState.Failed -> "Playlist unavailable: ${state.message}. Press Sync to retry."
}

/**
 * Fetches the playlist from the [Source] it is told to [use]. There is no local cache: the source is the truth, and a
 * failed fetch is reported as [PlaylistState.Failed] rather than hidden behind old data.
 *
 * The TV guide loads in the background after the playlist, so a sync always refreshes it too. It is
 * optional: until it arrives, or if it fails, [guide] is what it was (empty at first) and screens show
 * channel names only. While the app is in front, [keepGuideFresh] reloads it by itself, so it always
 * covers the next 24 hours. Only the guide is refreshed that way, never the playlist: the channels
 * must not change under a screen that is playing.
 */
class PlaylistViewModel : ViewModel() {

    private val _source = MutableStateFlow<Source?>(null)

    /** The source the state below belongs to. Null until the first [use]. */
    val source: StateFlow<Source?> = _source.asStateFlow()

    private val _state = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    private val _guide = MutableStateFlow(Guide.EMPTY)
    val guide: StateFlow<Guide> = _guide.asStateFlow()

    private var syncJob: Job? = null
    private var guideLoadedAt = 0L

    /** Switches to [source] and loads it. The same source again changes nothing: what is loaded stays. */
    fun use(source: Source) {
        if (source == _source.value) return
        _source.value = source
        sync()
    }

    /** Loads the guide for [playlist]. If that fails the guide stays as it was. */
    private suspend fun loadGuide(playlist: Playlist) {
        val url = playlist.epgUrl ?: return
        val ids = playlist.channels.mapNotNullTo(HashSet()) { it.tvgId?.takeIf(String::isNotBlank) }
        val now = System.currentTimeMillis()
        try {
            _guide.value = EpgLoader.load(url, ids, now)
            guideLoadedAt = now
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive() // a canceled load is not a guide failure
        }
    }

    /** Fetches the playlist and the guide again. Cancels a fetch that is still running. */
    fun sync() {
        val source = _source.value ?: return
        syncJob?.cancel()
        _state.value = PlaylistState.Loading
        _guide.value = Guide.EMPTY
        guideLoadedAt = 0
        syncJob = viewModelScope.launch {
            val playlist = try {
                PlaylistLoader.load(source)
            } catch (e: Exception) {
                ensureActive() // a canceled sync is not a failed playlist
                _state.value = PlaylistState.Failed(e.message ?: e.toString())
                return@launch
            }
            _state.value = PlaylistState.Loaded(playlist)
            loadGuide(playlist)
        }
    }

    /**
     * Reloads the guide when it is older than [GUIDE_MAX_AGE], and checks again every [GUIDE_CHECK_EVERY]
     * for as long as the caller runs it (the activity, while it is in front). A failed load is simply tried
     * again at the next check.
     */
    suspend fun keepGuideFresh() {
        while (true) {
            val playlist = (_state.value as? PlaylistState.Loaded)?.playlist
            val stale = System.currentTimeMillis() - guideLoadedAt > GUIDE_MAX_AGE.inWholeMilliseconds
            if (playlist != null && stale && syncJob?.isActive != true) {
                syncJob = viewModelScope.launch { loadGuide(playlist) }
            }
            delay(GUIDE_CHECK_EVERY)
        }
    }

    private companion object {
        val GUIDE_MAX_AGE = 1.hours
        val GUIDE_CHECK_EVERY = 10.minutes
    }
}
