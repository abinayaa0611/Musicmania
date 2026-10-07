package com.musicmania.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicmania.app.data.HomeData
import com.musicmania.app.data.MusicRepository
import com.musicmania.app.data.Song
import com.musicmania.app.data.UiState
import com.musicmania.app.data.toState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Everything that comes from the internet: home chart, search results, playlist tracks.
 * Each piece of state is a UiState (Loading / Success / Error) that screens simply `when` over.
 */
@OptIn(FlowPreview::class)
class ExplorerViewModel : ViewModel() {
    private val repo = MusicRepository()

    var home by mutableStateOf<UiState<HomeData>>(UiState.Loading); private set
    var search by mutableStateOf<UiState<List<Song>>?>(null); private set     // null = nothing typed
    val playlistTracks = mutableStateMapOf<Long, UiState<List<Song>>>()       // playlistId -> its tracks

    private val query = MutableStateFlow("")

    init {
        loadHome()
        viewModelScope.launch {
            // Debounce: wait 400ms after the last keystroke so we don't call the API on every letter.
            // collectLatest: if a new query arrives, the old (slower) request is cancelled.
            query.debounce(400).distinctUntilChanged().collectLatest { q ->
                if (q.isBlank()) {
                    search = null
                } else {
                    search = UiState.Loading
                    search = repo.search(q).toState()
                }
            }
        }
    }

    fun loadHome() {
        home = UiState.Loading
        viewModelScope.launch { home = repo.home().toState() }
    }

    fun onQuery(text: String) { query.value = text.trim() }

    /** Loads a playlist's songs once; pass force = true for "retry". */
    fun loadPlaylistTracks(id: Long, force: Boolean = false) {
        if (!force && playlistTracks[id] is UiState.Success) return
        playlistTracks[id] = UiState.Loading
        viewModelScope.launch { playlistTracks[id] = repo.albumTracks(id).toState() }
    }

    /** One-shot fetches used by "play" buttons. The result goes back through [onDone]. */
    fun fetchPlaylist(id: Long, onDone: (Result<List<Song>>) -> Unit) {
        viewModelScope.launch { onDone(repo.albumTracks(id)) }
    }

    fun fetchArtistTop(id: Long, onDone: (Result<List<Song>>) -> Unit) {
        viewModelScope.launch { onDone(repo.artistTop(id)) }
    }
}
