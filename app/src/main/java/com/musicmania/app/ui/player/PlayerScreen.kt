package com.musicmania.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicmania.app.PlayerViewModel
import com.musicmania.app.data.Song
import com.musicmania.app.ui.components.*
import com.musicmania.app.ui.theme.*

/**
 * FULL-SCREEN PLAYER (responsive).
 *  - Phone portrait: one scrolling column (header, record, controls).
 *  - Tablet / landscape (width >= 600dp and wider than tall): two columns, record left, controls right.
 */
@Composable
fun PlayerScreen(player: PlayerViewModel) {
    val song = player.current ?: return
    BackHandler(onBack = player::closePlayer)

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(hsv(song.hue, .6f, .30f), Bg)))
            .systemBarsPadding(),
    ) {
        val twoColumns = maxWidth >= 600.dp && maxWidth > maxHeight
        if (twoColumns) {
            val deck = (maxHeight - 100.dp).coerceIn(180.dp, 340.dp)
            Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Deck(song, player.isPlaying, deck) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PlayerHeader(player)
                    TrackControls(player, song)
                }
            }
        } else {
            val deck = (maxWidth - 80.dp).coerceIn(180.dp, 300.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp)) {
                PlayerHeader(player)
                Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp)) { Deck(song, player.isPlaying, deck) }
                TrackControls(player, song)
            }
        }
    }
}

@Composable
private fun PlayerHeader(player: PlayerViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        RText("⌄", Modifier.clickable { player.closePlayer() }.padding(horizontal = 10.dp), size = 24.sp)
        RText("NOW PLAYING", color = Amber, size = 11.sp, pixel = true)
        RText("⋯", Modifier.padding(horizontal = 10.dp), size = 22.sp)
    }
}

/** Title, equalizer, seek bar, transport buttons and the like/save/mix row. */
@Composable
private fun TrackControls(player: PlayerViewModel, song: Song) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                RText(song.title, size = 20.sp, pixel = true, maxLines = 2)
                RText(song.artist, Modifier.padding(top = 4.dp), Muted, 13.sp, maxLines = 1)
            }
            Equalizer(player.isPlaying)
        }

        val progress = player.positionSec / player.durationSec.coerceAtLeast(1f)
        PixelProgressBar(progress, onSeek = player::seekTo, Modifier.padding(top = 16.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            RText(formatTime(player.positionSec), color = Muted, size = 11.sp)
            RText(if (player.isBuffering) "BUFFERING..." else "30s PREVIEW", color = if (player.isBuffering) Amber else Muted, size = 10.sp, pixel = true)
            RText(formatTime(player.durationSec), color = Muted, size = 11.sp)
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            RText("◀◀", Modifier.clickable { player.rewind() }.padding(10.dp), size = 18.sp, pixel = true)
            Box(
                Modifier.size(76.dp)
                    .drawBehind { drawCircle(AmberDark, size.minDimension / 2, center + Offset(4.dp.toPx(), 4.dp.toPx())) }
                    .clip(CircleShape).background(Amber).clickable { player.togglePlay() },
                contentAlignment = Alignment.Center,
            ) { RText(if (player.isPlaying) "❚❚" else "▶\uFE0E", color = Color(0xFF1A0F00), size = 24.sp, pixel = true) }
            RText("▶▶", Modifier.clickable { player.skip(1) }.padding(10.dp), size = 18.sp, pixel = true)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val liked = player.isLiked(song.id)
            val saved = player.isSaved(song.id)
            ActionButton("♥ LIKE", liked, Pink, Color(0xFF6B1B36), Modifier.weight(1f)) { player.toggleLike(song) }
            ActionButton(if (saved) "✓ SAVED" else "↓ SAVE", saved, Green, Color(0xFF1A5A22), Modifier.weight(1f)) { player.toggleSaved(song) }
            ActionButton("+ MY MIX", false, Amber, AmberDark, Modifier.weight(1f)) { player.addToMix(song) }
        }
    }
}

/** Vinyl + tonearm, sized by [size] so it adapts to the screen. */
@Composable
private fun Deck(song: Song, playing: Boolean, size: Dp) {
    Box(Modifier.size(size)) {
        Vinyl(song, playing, Modifier.fillMaxSize())
        Tonearm(playing, size * .56f, Modifier.align(Alignment.TopEnd))
    }
}

/** Spinning record. The Animatable runs only while playing; pausing cancels it and freezes the angle. */
@Composable
private fun Vinyl(song: Song, playing: Boolean, modifier: Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) while (true) {
            val start = rotation.value % 360f
            rotation.snapTo(start)
            rotation.animateTo(start + 360f, tween(7000, easing = LinearEasing))
        }
    }
    val glow = hsv(song.hue, .8f, .6f)
    Box(modifier.shadow(24.dp, CircleShape, ambientColor = glow, spotColor = glow).graphicsLayer { rotationZ = rotation.value }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFF0A0810))
            var radius = r * .3f
            while (radius < r) { drawCircle(Color.White.copy(alpha = .05f), radius, style = Stroke(1.dp.toPx())); radius += 3.dp.toPx() }
            drawCircle(Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = .16f), Color.Transparent, Color.Transparent, Color.White.copy(alpha = .14f), Color.Transparent)), r)
        }
        CoverImage(song.coverUrl, song.hue, song.glyph, Modifier.fillMaxSize(.44f), CircleShape, 40.sp)   // album art = record label
        Box(Modifier.size(12.dp).background(Bg, CircleShape))
    }
}

@Composable
private fun Tonearm(playing: Boolean, length: Dp, modifier: Modifier) {
    val angle by animateFloatAsState(if (playing) 12f else -22f, tween(700), label = "arm")
    Box(
        modifier.offset(10.dp, (-8).dp).size(8.dp, length)
            .graphicsLayer { transformOrigin = TransformOrigin(.5f, .05f); rotationZ = angle }
            .background(Cream, RoundedCornerShape(4.dp)),
    ) {
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(20.dp).background(Amber, CircleShape))
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 6.dp).size(14.dp, 12.dp).background(Pink))
    }
}

@Composable
private fun Equalizer(playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "eq")
    val colors = listOf(Amber, Pink, Cyan, Green)
    val durations = listOf(800, 650, 900, 720)
    Row(Modifier.height(26.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        colors.forEachIndexed { i, color ->
            val s = transition.animateFloat(.2f, 1f, infiniteRepeatable(tween(durations[i]), RepeatMode.Reverse), label = "bar$i")
            Box(Modifier.width(5.dp).fillMaxHeight().graphicsLayer { scaleY = if (playing) s.value else .2f; transformOrigin = TransformOrigin(.5f, 1f) }.background(color))
        }
    }
}

/** Segmented pixel seek bar: tap or drag to seek. */
@Composable
private fun PixelProgressBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier.fillMaxWidth().height(14.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek(it.x / size.width) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onSeek(change.position.x / size.width) } },
    ) {
        val seg = 6.dp.toPx()
        val gap = 2.dp.toPx()
        val count = (size.width / (seg + gap)).toInt()
        val filled = (count * progress).toInt()
        repeat(count) { i -> drawRect(if (i < filled) Amber else Color(0xFF4A3C63), Offset(i * (seg + gap), 0f), Size(seg, size.height)) }
    }
}

@Composable
private fun ActionButton(label: String, active: Boolean, color: Color, shadow: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.then(if (active) Modifier.pixelShadow(shadow) else Modifier)
            .background(Color.Black.copy(alpha = .25f))
            .border(2.dp, if (active) color else Color(0xFF4A3C63), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { RText(label, color = if (active) color else Cream, size = 10.sp, pixel = true) }
}
