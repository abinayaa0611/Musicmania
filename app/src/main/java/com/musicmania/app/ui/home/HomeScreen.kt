package com.musicmania.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.musicmania.app.ExplorerViewModel
import com.musicmania.app.PlayerViewModel
import com.musicmania.app.data.HomeData
import com.musicmania.app.data.Playlist
import com.musicmania.app.data.UiState
import com.musicmania.app.data.userMessage
import com.musicmania.app.ui.LocalBottomInset
import com.musicmania.app.ui.components.*
import com.musicmania.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** HOME: shows Loading / Error / Content depending on the API request state. */
@Composable
fun HomeScreen(explorer: ExplorerViewModel, player: PlayerViewModel) {
    when (val state = explorer.home) {
        UiState.Loading -> LoadingBlock()
        is UiState.Error -> ErrorBox(state.message, onRetry = explorer::loadHome)
        is UiState.Success -> HomeContent(state.data, explorer, player)
    }
}

/**
 * The curved CD wheel (same math as before) but now:
 *  - discs are real playlists from the API, with their cover image as the CD label
 *  - the disc size comes from the available width, so it scales on phones and tablets
 *  - "play" fetches the playlist's songs from the API first
 */
@Composable
private fun HomeContent(data: HomeData, explorer: ExplorerViewModel, player: PlayerViewModel) {
    val playlists = remember(data) { data.playlists.take(8) }
    val pos = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    val centered = if (playlists.isEmpty()) 0 else pos.value.roundToInt().coerceIn(0, playlists.lastIndex)

    fun playPlaylist(p: Playlist) {
        if (loading) return
        loading = true
        explorer.fetchPlaylist(p.id) { result ->
            loading = false
            result.onSuccess { songs -> if (songs.isEmpty()) player.showToast("This tape is empty") else player.play(songs, 0) }
                .onFailure { player.showToast(it.userMessage()) }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 6.dp, bottom = LocalBottomInset.current)) {

        if (playlists.isNotEmpty()) {
            // ---- the wheel ----
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val disc = (maxWidth * .42f).coerceIn(130.dp, 200.dp)   // responsive disc size
                val radius = disc * 2f                                  // radius of the big circle
                Box(
                    Modifier.fillMaxWidth().height(disc * 1.43f).clipToBounds().pointerInput(disc, playlists.size) {
                        val settle: () -> Unit = {
                            scope.launch { pos.animateTo(pos.value.roundToInt().toFloat(), spring(stiffness = Spring.StiffnessLow)) }
                        }
                        detectHorizontalDragGestures(onDragEnd = settle, onDragCancel = settle) { change, dragX ->
                            change.consume()
                            val next = (pos.value - dragX / (disc.toPx() * .7f)).coerceIn(0f, playlists.lastIndex.toFloat())
                            scope.launch { pos.snapTo(next) }
                        }
                    },
                ) {
                    playlists.forEachIndexed { i, playlist ->
                        val d = i - pos.value
                        val ad = abs(d)
                        if (ad < 3f) {
                            val angle = (d * 32f).toDouble() * PI / 180.0
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter).padding(top = 14.dp).size(disc)
                                    .offset { IntOffset((radius.toPx() * sin(angle)).roundToInt(), (radius.toPx() * (1 - cos(angle))).roundToInt()) }
                                    .zIndex(10f - ad)
                                    .graphicsLayer {
                                        rotationZ = d * 32f
                                        val s = 1f - min(ad, 2.5f) * .13f
                                        scaleX = s; scaleY = s
                                        alpha = (1f - ad * .28f).coerceIn(0f, 1f)
                                    }
                                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                                        if (i == centered) playPlaylist(playlist)
                                        else scope.launch { pos.animateTo(i.toFloat(), spring()) }
                                    },
                            ) { CdDisc(playlist, spinning = i == centered, Modifier.fillMaxSize()) }
                        }
                    }
                }
            }

            // ---- info for the centred playlist ----
            val selected = playlists[centered]
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RText(selected.title, color = Amber, size = 20.sp, pixel = true, maxLines = 1)
                RText(if (selected.trackCount > 0) "${selected.trackCount} tracks" else "Playlist", Modifier.padding(top = 4.dp, bottom = 12.dp), Muted, 12.sp)
                PixelButton(if (loading) "LOADING..." else "▶\uFE0E PLAY TAPE") { playPlaylist(selected) }
            }
        }

        // ---- suggestions: trending songs ----
        SectionTitle("PICKED FOR YOU", Modifier.padding(horizontal = 16.dp))
        val trending = data.tracks.take(12)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(trending) { index, song ->
                Column(Modifier.width(116.dp).clickable { player.play(trending, index) }) {
                    CoverImage(song.coverUrl, song.hue, song.glyph, Modifier.size(116.dp), glyphSize = 40.sp)
                    RText(song.title, Modifier.padding(top = 7.dp), size = 12.sp, maxLines = 1)
                    RText(song.artist, color = Muted, size = 11.sp, maxLines = 1)
                }
            }
        }

        // ---- suggestions: artists (tap = play their top songs) ----
        if (data.artists.isNotEmpty()) {
            SectionTitle("ARTISTS ON REPEAT", Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(data.artists) { _, artist ->
                    Column(
                        Modifier.width(78.dp).clickable {
                            explorer.fetchArtistTop(artist.id) { r ->
                                r.onSuccess { if (it.isEmpty()) player.showToast("No previews for ${artist.name}") else player.play(it, 0) }
                                    .onFailure { e -> player.showToast(e.userMessage()) }
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CoverImage(artist.pictureUrl, artist.hue, artist.initials, Modifier.size(78.dp), CircleShape, 22.sp)
                        RText(artist.name, Modifier.padding(top = 6.dp), size = 11.sp, maxLines = 1)
                        RText("Artist", color = Muted, size = 10.sp)
                    }
                }
            }
        }
    }
}

/** A CD: shiny sweep gradient + grooves, the playlist cover as its circular label, a hole in the middle. */
@Composable
private fun CdDisc(playlist: Playlist, spinning: Boolean, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "cdSpin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "cdAngle",
    )
    val h = playlist.hue
    Box(modifier.graphicsLayer { rotationZ = if (spinning) spin.value else 0f }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.sweepGradient(listOf(hsv(h, .8f, .95f), hsv(h + 60, .8f, .8f), Color.White.copy(alpha = .85f), hsv(h, .7f, .55f), hsv(h + 120, .8f, .9f), hsv(h, .8f, .95f))), r)
            for (k in 1..14) drawCircle(Color.Black.copy(alpha = .1f), r * (.3f + .05f * k), Offset(r, r), style = Stroke(1f))
        }
        CoverImage(playlist.coverUrl, playlist.hue, playlist.glyph, Modifier.fillMaxSize(.62f), CircleShape, 26.sp)
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Bg, r * .12f, Offset(r, r))
            drawCircle(Color.White.copy(alpha = .35f), r * .12f, Offset(r, r), style = Stroke(r * .04f))
        }
    }
}
