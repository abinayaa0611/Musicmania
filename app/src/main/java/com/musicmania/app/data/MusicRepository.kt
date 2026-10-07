package com.musicmania.app.data

import com.musicmania.app.data.remote.AppleMusicApi
import com.musicmania.app.data.remote.AppleNetwork
import com.musicmania.app.data.remote.AppleTrackDto
import kotlinx.coroutines.CancellationException
import java.io.IOException

fun Throwable.userMessage(): String = when (this) {
    is IOException -> "NO CONNECTION — CHECK INTERNET AND RETRY"
    else -> "APPLE MUSIC CATALOG ERROR — RETRY"
}

fun <T> Result<T>.toState(): UiState<T> = fold({ UiState.Success(it) }, { UiState.Error(it.userMessage()) })

class MusicRepository(private val api: AppleMusicApi = AppleNetwork.api) {
    suspend fun search(query: String): Result<List<Song>> = safe {
        api.search(query).results.mapNotNull { it.toSong() }
    }

    suspend fun albumTracks(id: Long): Result<List<Song>> = safe {
        api.lookupAlbum(id).results.filter { it.trackId != null && it.previewUrl != null }.mapNotNull { it.toSong() }
    }

    suspend fun artistTop(id: Long): Result<List<Song>> = safe {
        api.lookupArtistSongs(id).results
            .filter { it.wrapperType == "track" && it.previewUrl != null }
            .mapNotNull { it.toSong() }
            .distinctBy { it.id }
            .take(25)
    }

    suspend fun home(): Result<HomeData> = safe {
        // These are search categories, not hard-coded tracks. Every displayed song is returned live by Apple.
        val terms = listOf("top hits", "pop", "rock")
        val responses = terms.map { api.search(it, limit = 12).results }
        val tracks = responses.flatten().distinctBy { it.trackId }.mapNotNull { it.toSong() }.take(24)
        val artists = tracks.groupBy { it.artistId to it.artist }.map { (key, songs) ->
            Artist(key.first, key.second, songs.firstOrNull()?.coverUrl)
        }.take(12)
        val albums = tracks.filter { it.albumId != 0L && it.album.isNotBlank() }
            .groupBy { it.albumId }
            .values.map { songs ->
                val first = songs.first()
                Playlist(first.albumId, first.album, first.coverUrl, songs.size)
            }.take(8)
        check(tracks.isNotEmpty()) { "Apple returned no playable previews" }
        HomeData(tracks, artists, albums)
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun String?.secure(): String? = this?.takeIf { it.isNotBlank() }?.replaceFirst("http://", "https://")

private fun AppleTrackDto.toSong(): Song? {
    val id = trackId ?: return null
    val preview = previewUrl.secure() ?: return null
    val title = trackName?.takeIf { it.isNotBlank() } ?: return null
    return Song(
        id = id,
        title = title,
        artist = artistName,
        artistId = artistId ?: 0,
        coverUrl = artworkUrl100?.secure()?.replace("100x100", "600x600") ?: artworkUrl100?.secure(),
        previewUrl = preview,
        durationSec = ((trackTimeMillis ?: 30000L) / 1000L).toInt().coerceAtLeast(1),
        albumId = collectionId ?: 0,
        album = collectionName.orEmpty(),
    )
}
