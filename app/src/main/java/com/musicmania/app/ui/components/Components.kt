package com.musicmania.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicmania.app.data.Song
import com.musicmania.app.ui.theme.*

// ---------- tiny helpers ----------

/** HSV colour with the hue wrapped into 0..360 so callers can add offsets freely. */
fun hsv(h: Float, s: Float, v: Float): Color = Color.hsv(((h % 360f) + 360f) % 360f, s, v)

fun formatTime(sec: Float): String {
    val s = sec.toInt()
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** The retro "hard drop shadow": a solid offset rectangle drawn BEHIND the element. */
fun Modifier.pixelShadow(color: Color, offset: Dp = 3.dp): Modifier = drawBehind {
    val o = offset.toPx()
    drawRect(color, topLeft = Offset(o, o), size = size)
}

/** Dotted divider along the bottom edge (like perforated tape). */
fun Modifier.dottedBottom(color: Color): Modifier = drawBehind {
    drawLine(
        color, Offset(0f, size.height), Offset(size.width, size.height),
        strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
    )
}

/** One text composable for the whole app so fonts/sizes stay consistent. */
@Composable
fun RText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Cream,
    size: TextUnit = 13.sp,
    pixel: Boolean = false,          // true = pixel heading font
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color, fontSize = size, textAlign = align,
            fontFamily = if (pixel) PixelFont else BodyFont,
            fontWeight = if (pixel) FontWeight.Bold else FontWeight.Normal,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

// ---------- building blocks ----------

/** Generated "album art": gradient + checkerboard overlay (the pixel look) + a glyph. */
@Composable
fun PixelArt(
    hue: Float,
    glyph: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(2.dp),
    glyphSize: TextUnit = 18.sp,
) {
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(hsv(hue, .7f, .95f), hsv(hue + 50f, .8f, .4f))))
            .drawBehind {
                val cell = 8.dp.toPx()
                val cols = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                for (r in 0 until rows) for (c in 0 until cols) {
                    if ((r + c) % 2 == 0) drawRect(Color.Black.copy(alpha = .16f), Offset(c * cell, r * cell), Size(cell, cell))
                }
            },
        contentAlignment = Alignment.Center,
    ) { RText(glyph, color = Color.White.copy(alpha = .85f), size = glyphSize, pixel = true) }
}

/** Section heading followed by a dotted line. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        RText(title, pixel = true, size = 13.sp)
        Box(Modifier.weight(1f).padding(start = 8.dp).height(2.dp).dottedBottom(Line))
    }
}

/** Amber button that "presses in" visually via its shadow. */
@Composable
fun PixelButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.pixelShadow(AmberDark).background(Amber).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { RText(label, color = Color(0xFF1A0F00), size = 12.sp, pixel = true) }
}

/** A row in any song list: number, cover, title/artist, length, heart. */
@Composable
fun SongRow(
    song: Song, number: Int, isCurrent: Boolean, isLiked: Boolean,
    onClick: () -> Unit, onLike: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).dottedBottom(Line).padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RText(number.toString().padStart(2, '0'), Modifier.width(18.dp), Muted, 10.sp, pixel = true)
        CoverImage(song.coverUrl, song.hue, song.glyph, Modifier.size(44.dp), glyphSize = 16.sp)
        Column(Modifier.weight(1f)) {
            RText(song.title, color = if (isCurrent) Amber else Cream, maxLines = 1)
            RText(song.artist, color = Muted, size = 11.sp, maxLines = 1)
        }
        RText(formatTime(song.durationSec.toFloat()), color = Muted, size = 10.sp)
        RText("♥", Modifier.clickable(onClick = onLike).padding(4.dp), if (isLiked) Pink else Color(0xFF4A3C63), 18.sp)
    }
}

/** The creative search bar: ">_" prompt, hard shadow, blinking block cursor when idle. */
@Composable
fun RetroSearchBar(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val accent = if (focused) Cyan else Amber
    val shadow = if (focused) Color(0xFF0B5D75) else AmberDark
    // An infinite transition drives the blink; reading it in graphicsLayer avoids recomposing every frame.
    val blink = rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    Row(
        modifier.fillMaxWidth().height(42.dp).pixelShadow(shadow, 4.dp).background(Panel)
            .border(2.dp, accent).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RText(">_", color = accent, size = 12.sp, pixel = true)
        BasicTextField(
            value = query, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = Cream, fontSize = 13.sp, fontFamily = BodyFont),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RText("search tunes, tapes, artists", color = Muted)
                        if (!focused) Box(Modifier.size(8.dp, 16.dp).graphicsLayer { alpha = blink.value }.background(Amber))
                    }
                    inner()
                }
            },
        )
    }
}

/**
 * Picture from the internet (loaded + cached by Coil) drawn over the pixel-art placeholder.
 * While loading, or if loading fails (offline), the pixel art stays visible, so there is never a blank hole.
 * A faint scanline overlay keeps real photos in the retro theme.
 */
@Composable
fun CoverImage(
    url: String?,
    hue: Float,
    glyph: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(2.dp),
    glyphSize: TextUnit = 18.sp,
) {
    Box(modifier.clip(shape)) {
        PixelArt(hue, glyph, Modifier.fillMaxSize(), RectangleShape, glyphSize)
        if (url != null) {
            val context = LocalContext.current
            val request = remember(url) { ImageRequest.Builder(context).data(url).crossfade(300).build() }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                    val step = 3.dp.toPx()
                    var y = 0f
                    while (y < size.height) {          // scanlines
                        drawRect(Color.Black.copy(alpha = .10f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                        y += step
                    }
                },
            )
        }
    }
}

/** Pulsing placeholder rows shown while the API request is in flight. */
@Composable
fun LoadingBlock(modifier: Modifier = Modifier, rows: Int = 5) {
    val pulse = rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = .25f, targetValue = .6f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RText("LOADING TAPES...", color = Amber, size = 11.sp, pixel = true)
        repeat(rows) { Box(Modifier.fillMaxWidth().height(44.dp).graphicsLayer { alpha = pulse.value }.background(Panel2)) }
    }
}

/** Error message + retry button. Pass onRetry = null to hide the button. */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RText("SIGNAL LOST", color = Pink, size = 14.sp, pixel = true)
        RText(message, color = Muted, size = 12.sp, align = TextAlign.Center)
        if (onRetry != null) PixelButton("RETRY", onClick = onRetry)
    }
}
