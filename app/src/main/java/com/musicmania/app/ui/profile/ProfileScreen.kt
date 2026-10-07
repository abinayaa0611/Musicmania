package com.musicmania.app.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicmania.app.PlayerViewModel
import androidx.compose.ui.platform.LocalContext
import com.musicmania.app.data.LocalStore
import com.musicmania.app.data.Days
import com.musicmania.app.ui.LocalBottomInset
import com.musicmania.app.ui.components.*
import com.musicmania.app.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

// Colours for activity levels 0..4 (dark -> glowing green), like a GitHub contribution graph.
private val LevelColors = listOf(Color(0xFF221A31), Color(0xFF1F5A2E), Color(0xFF2F9A46), Color(0xFF4FD266), Color(0xFF8DFF9C))
private const val WEEKS = 20

private val Quotes = linkedMapOf(
    "Nostalgic" to "Some songs are time machines with a scratchy intro. You keep pressing rewind on purpose.",
    "Chill" to "Slow tapes, soft light. The world can wait until side B.",
    "Hype" to "Turn it up until the walls remember your name.",
    "Focus" to "One track, one thought, no static. You hit your stride after midnight.",
)

/** Seconds listened in a day -> activity level 0..4. */
private fun levelFor(seconds: Float): Int = when {
    seconds < 1f -> 0
    seconds < 300f -> 1      // under 5 min
    seconds < 900f -> 2      // under 15 min
    seconds < 1800f -> 3     // under 30 min
    else -> 4
}

/**
 * PROFILE: now driven by REAL data. The player counts every second you actually listen
 * (PlayerViewModel.log) and this screen turns it into hours, a streak and the glowing grid.
 */
@Composable
fun ProfileScreen(player: PlayerViewModel) {
    val context = LocalContext.current
    val store = remember(context) { LocalStore(context) }
    val profileName = remember { store.getProfileName() }
    // The 140 date keys never change during a session, so build them once.
    val dayKeys = remember { List(WEEKS * 7) { Days.key(WEEKS * 7 - 1 - it) } }   // oldest ... today
    val log = player.log
    val levels = dayKeys.map { levelFor(log[it] ?: 0f) }
    val streak = levels.reversed().takeWhile { it > 0 }.size
    val activeDays = levels.count { it > 0 }
    val totalSec = log.values.sum()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = LocalBottomInset.current)) {

        // ---- device-local profile: persists on this installation ----
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PixelArt(340f, "M", Modifier.size(76.dp).border(3.dp, Amber, CircleShape), CircleShape, 26.sp)
            Column {
                RText(profileName, size = 18.sp, pixel = true)
                RText("THIS DEVICE", Modifier.padding(top = 3.dp), Muted, 11.sp)
                RText("LOCAL PROFILE · YOUR DATA STAYS ON THIS DEVICE", Modifier.padding(top = 3.dp), Muted, 9.sp)
            }
        }

        // ---- stats ----
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(formatListening(totalSec), "TIME PLAYED", Modifier.weight(1f))
            StatCard(streak.toString(), "DAY STREAK", Modifier.weight(1f))
            StatCard(player.liked.size.toString(), "SONGS LIKED", Modifier.weight(1f))
        }

        // ---- activity grid ----
        SectionTitle("LISTENING LOG")
        Column(Modifier.fillMaxWidth().background(Panel).border(2.dp, Color(0xFF31254A)).padding(12.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                ActivityGrid(levels)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RText(if (activeDays == 0) "Play a song to light your first day" else "$activeDays days active", color = Muted, size = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    RText("less", color = Muted, size = 10.sp)
                    LevelColors.forEach { Box(Modifier.size(10.dp).background(it)) }
                    RText("more", color = Muted, size = 10.sp)
                }
            }
        }

        // ---- mood quote ----
        SectionTitle("YOUR MOOD TAPE")
        var mood by rememberSaveable { mutableStateOf("Nostalgic") }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Quotes.keys.forEach { m ->
                val on = m == mood
                Box(
                    Modifier.then(if (on) Modifier.pixelShadow(Color(0xFF6B1B36)) else Modifier)
                        .border(2.dp, if (on) Pink else Line).clickable { mood = m }.padding(horizontal = 10.dp, vertical = 7.dp),
                ) { RText(m.uppercase(), color = if (on) Pink else Muted, size = 10.sp, pixel = true) }
            }
        }
        var typed by remember { mutableStateOf("") }
        LaunchedEffect(mood) {                       // typewriter: restart whenever the mood changes
            val full = Quotes.getValue(mood)
            typed = ""
            for (i in 1..full.length) { typed = full.take(i); delay(22) }
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp).background(Panel)
                .drawBehind { drawRect(Pink, size = Size(4.dp.toPx(), size.height)) }
                .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp).heightIn(min = 60.dp),
        ) { RText("${typed}_", size = 13.sp) }

        RText("Music catalog and 30-second previews by Apple iTunes Search API", Modifier.padding(top = 24.dp), Muted, 9.sp)
    }
}

private fun formatListening(totalSec: Float): String {
    val minutes = (totalSec / 60f).toInt()
    return if (minutes < 60) "${minutes}m" else String.format(Locale.US, "%.1fh", totalSec / 3600f)
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.background(Panel).border(2.dp, Color(0xFF31254A)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RText(value, color = Amber, size = 20.sp, pixel = true)
        RText(label, color = Muted, size = 9.sp)
    }
}

/** One Canvas for all 140 cells; cells pop in one after another (progress 0 -> 1). */
@Composable
private fun ActivityGrid(levels: List<Int>) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400)) }

    Canvas(Modifier.size((WEEKS * 14 - 3).dp, (7 * 14 - 3).dp)) {
        val cell = 11.dp.toPx()
        val step = 14.dp.toPx()
        val glow = 2.dp.toPx()
        levels.forEachIndexed { index, level ->
            val col = index / 7
            val row = index % 7
            val scale = (progress.value * 1.5f - index / levels.size.toFloat() * .5f).coerceIn(0f, 1f)
            val s = cell * scale
            val topLeft = Offset(col * step + (cell - s) / 2, row * step + (cell - s) / 2)
            val color = LevelColors[level]
            if (level >= 3) drawRoundRect(color.copy(alpha = .35f * scale), Offset(topLeft.x - glow, topLeft.y - glow), Size(s + glow * 2, s + glow * 2), CornerRadius(3.dp.toPx()))
            drawRoundRect(color, topLeft, Size(s, s), CornerRadius(2.dp.toPx()))
        }
    }
}
