package com.musicmania.app.data

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val coverUrl: String? = null,
    val previewUrl: String,
    val durationSec: Int = 30,
    val albumId: Long = 0,
    val album: String = "",
) {
    val hue: Float get() = (abs(id) % 360L).toFloat()
    val glyph: String get() = title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪"
}

data class Playlist(val id: Long, val title: String, val coverUrl: String?, val trackCount: Int) {
    val hue: Float get() = (abs(id) % 360L).toFloat()
    val glyph: String get() = "♫"
}

data class Artist(val id: Long, val name: String, val pictureUrl: String?) {
    val hue: Float get() = (abs(id) % 360L).toFloat()
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

data class HomeData(val tracks: List<Song>, val artists: List<Artist>, val playlists: List<Playlist>)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
