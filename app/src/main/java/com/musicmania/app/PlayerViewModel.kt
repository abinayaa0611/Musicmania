package com.musicmania.app

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicmania.app.data.Days
import com.musicmania.app.data.LocalStore
import com.musicmania.app.data.Song
import com.musicmania.app.player.MusicPlayerService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private var restoring = false

    var current by mutableStateOf<Song?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var isBuffering by mutableStateOf(false); private set
    var positionSec by mutableFloatStateOf(0f); private set
    var durationSec by mutableFloatStateOf(30f); private set
    var playerOpen by mutableStateOf(false); private set
    var toast by mutableStateOf<String?>(null); private set

    val liked = mutableStateListOf<Song>().apply { addAll(store.loadSongs("liked")) }
    val saved = mutableStateListOf<Song>().apply { addAll(store.loadSongs("saved")) }
    val mix = mutableStateListOf<Song>().apply { addAll(store.loadSongs("mix")) }
    var log by mutableStateOf(store.loadLog()); private set
    private var unsavedSec = 0f

    init {
        val token = SessionToken(app, ComponentName(app, MusicPlayerService::class.java))
        MediaController.Builder(app, token).buildAsync().apply {
            addListener({
                controller = get()
                attachController(controller!!)
                restoreLastQueue()
            }, MoreExecutors.directExecutor())
        }
        viewModelScope.launch {
            while (true) {
                delay(250)
                val c = controller ?: continue
                positionSec = (c.currentPosition.coerceAtLeast(0L) / 1000f)
                val d = c.duration
                if (d != C.TIME_UNSET && d > 0) durationSec = d / 1000f
                if (c.isPlaying) addListened(.25f)
            }
        }
    }

    private fun attachController(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                val d = c.duration
                if (d != C.TIME_UNSET && d > 0) durationSec = d / 1000f
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val index = c.currentMediaItemIndex
                current = queue.getOrNull(index)
                positionSec = 0f
                current?.let { store.setLastSong(it, index, queue) }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                toast = "CAN'T PLAY THIS PREVIEW"
                isPlaying = false
            }
        })
    }

    private fun mediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setUri(song.previewUrl)
        .setMediaId(song.id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.coverUrl?.let(android.net.Uri::parse))
                .build()
        ).build()

    fun play(songs: List<Song>, index: Int) {
        val c = controller ?: run { toast = "PLAYER STARTING… TRY AGAIN"; return }
        if (songs.isEmpty()) return
        queue = songs
        val start = index.coerceIn(0, songs.lastIndex)
        c.setMediaItems(songs.map(::mediaItem), start, 0L)
        c.prepare()
        c.play()
        current = songs[start]
        durationSec = songs[start].durationSec.toFloat().coerceAtLeast(1f)
        positionSec = 0f
        playerOpen = true
        store.setLastSong(current!!, start, queue)
    }

    private fun restoreLastQueue() {
        if (restoring) return
        restoring = true
        val savedQueue = store.loadLastQueue()
        if (savedQueue.isNotEmpty()) {
            queue = savedQueue
            current = savedQueue.getOrNull(store.lastIndex())
            current?.let { durationSec = it.durationSec.toFloat().coerceAtLeast(1f) }
        }
    }

    fun skip(step: Int) {
        controller?.let { if (step > 0) it.seekToNextMediaItem() else it.seekToPreviousMediaItem(); it.play() }
    }
    fun rewind() {
        if (positionSec > 3f) controller?.seekTo(0) else skip(-1)
    }
    fun togglePlay() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        controller?.seekTo((f * durationSec * 1000).toLong())
        positionSec = f * durationSec
    }
    fun openPlayer() { if (current != null) playerOpen = true }
    fun closePlayer() { playerOpen = false }

    fun isLiked(id: Long) = liked.any { it.id == id }
    fun isSaved(id: Long) = saved.any { it.id == id }
    fun toggleLike(song: Song) = toggle(liked, "liked", song, "ADDED TO LIKED SONGS")
    fun toggleSaved(song: Song) = toggle(saved, "saved", song, "SAVED TO LIBRARY")
    fun addToMix(song: Song) {
        if (mix.any { it.id == song.id }) { toast = "ALREADY IN MY MIX"; return }
        mix.add(0, song); store.saveSongs("mix", mix.toList()); toast = "ADDED TO MY MIX"
    }
    private fun toggle(list: SnapshotStateList<Song>, key: String, song: Song, addedMessage: String) {
        if (list.any { it.id == song.id }) { list.removeAll { it.id == song.id } } else { list.add(0, song); toast = addedMessage }
        store.saveSongs(key, list.toList())
    }
    fun showToast(message: String) { toast = message }
    fun clearToast() { toast = null }

    private fun addListened(sec: Float) {
        val key = Days.key(0)
        log = log + (key to ((log[key] ?: 0f) + sec))
        unsavedSec += sec
        if (unsavedSec >= 10f) flushLog()
    }
    private fun flushLog() { store.saveLog(log); unsavedSec = 0f }

    override fun onCleared() {
        flushLog()
        controller?.release()
        controller = null
        super.onCleared()
    }
}
