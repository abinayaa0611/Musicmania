package com.musicmania.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicmania.app.ExplorerViewModel
import com.musicmania.app.PlayerViewModel
import com.musicmania.app.data.Song
import com.musicmania.app.data.UiState
import com.musicmania.app.ui.LocalBottomInset
import com.musicmania.app.ui.components.*
import com.musicmania.app.ui.theme.*

/**
 * LIBRARY
 *  - PLAYLISTS: "My Mix" (yours, stored on the device) + editor playlists from the API.
 *               Opening an API playlist fetches its songs the first time.
 *  - LIKED / SAVED: your hearts and bookmarks, stored on the device.
 */
@Composable
fun LibraryScreen(explorer: ExplorerViewModel, player: PlayerViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(setOf<Long>()) }     // ids of open playlists (-1 = My Mix)
    val apiPlaylists = (explorer.home as? UiState.Success)?.data?.playlists.orEmpty()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = LocalBottomInset.current)) {

        // ---- cassette tabs ----
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("PLAYLISTS", "LIKED", "SAVED").forEachIndexed { i, label ->
                val on = i == tab
                Box(
                    Modifier.weight(1f)
                        .then(if (on) Modifier.drawBehind { drawRect(AmberDark, Offset(0f, 3.dp.toPx()), size) } else Modifier)
                        .background(if (on) Amber else Panel).border(2.dp, if (on) Amber else Line)
                        .clickable { tab = i }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { RText(label, color = if (on) Color(0xFF1A0F00) else Muted, size = 10.sp, pixel = true) }
            }
        }

        when (tab) {
            0 -> {
                // My Mix (local)
                val mixOpen = -1L in expanded
                PlaylistBlock(
                    title = "My Mix", subtitle = "${player.mix.size} songs · yours",
                    cover = { PixelArt(120f, "+", Modifier.size(52.dp), CircleShape, 20.sp) },
                    open = mixOpen, onToggle = { expanded = if (mixOpen) expanded - -1L else expanded + -1L },
                ) { SongList(player, player.mix.toList()) }

                // Playlists from the API
                apiPlaylists.forEach { p ->
                    val open = p.id in expanded
                    PlaylistBlock(
                        title = p.title, subtitle = if (p.trackCount > 0) "${p.trackCount} songs" else "Playlist",
                        cover = { CoverImage(p.coverUrl, p.hue, p.glyph, Modifier.size(52.dp), CircleShape, 20.sp) },
                        open = open,
                        onToggle = {
                            expanded = if (open) expanded - p.id else expanded + p.id
                            if (!open) explorer.loadPlaylistTracks(p.id)     // fetch songs on first open
                        },
                    ) {
                        when (val s = explorer.playlistTracks[p.id]) {
                            null, UiState.Loading -> LoadingBlock(rows = 3)
                            is UiState.Error -> ErrorBox(s.message, onRetry = { explorer.loadPlaylistTracks(p.id, force = true) })
                            is UiState.Success -> SongList(player, s.data)
                        }
                    }
                }
                if (apiPlaylists.isEmpty()) RText("Connect to the internet to see editor playlists.", Modifier.padding(8.dp), Muted, 11.sp)
            }
            1 -> SongList(player, player.liked.toList(), "Tap a heart on any song to add it here.")
            else -> SongList(player, player.saved.toList(), "Use SAVE in the player to keep songs here.")
        }
    }
}

/** A playlist card that expands to show [content] underneath with a slide animation. */
@Composable
private fun PlaylistBlock(
    title: String, subtitle: String, cover: @Composable () -> Unit,
    open: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    val arrow by animateFloatAsState(if (open) 90f else 0f, label = "arrow")
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Panel).border(2.dp, Color(0xFF31254A), RoundedCornerShape(4.dp))
            .clickable(onClick = onToggle).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cover()
        Column(Modifier.weight(1f)) {
            RText(title, maxLines = 1)
            RText(subtitle, color = Muted, size = 11.sp)
        }
        RText("▶\uFE0E", Modifier.graphicsLayer { rotationZ = arrow }, Amber)
    }
    AnimatedVisibility(open) {
        Column(Modifier.padding(bottom = 8.dp, start = 8.dp).drawBehind {
            drawLine(Amber, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx())     // vintage side rail
        }.padding(start = 8.dp), content = content)
    }
}

/** Rows for [songs]; tapping row i starts this list as the queue at position i. */
@Composable
private fun SongList(player: PlayerViewModel, songs: List<Song>, emptyHint: String = "Nothing here yet.") {
    if (songs.isEmpty()) RText(emptyHint, Modifier.padding(8.dp), Muted, 11.sp)
    songs.forEachIndexed { index, song ->
        SongRow(
            song, index + 1, isCurrent = player.current?.id == song.id, isLiked = player.isLiked(song.id),
            onClick = { player.play(songs, index) }, onLike = { player.toggleLike(song) },
        )
    }
}
