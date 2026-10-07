package com.musicmania.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicmania.app.ExplorerViewModel
import com.musicmania.app.PlayerViewModel
import com.musicmania.app.data.Song
import com.musicmania.app.data.UiState
import com.musicmania.app.ui.components.*
import com.musicmania.app.ui.home.HomeScreen
import com.musicmania.app.ui.library.LibraryScreen
import com.musicmania.app.ui.player.PlayerScreen
import com.musicmania.app.ui.profile.ProfileScreen
import com.musicmania.app.ui.theme.*
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: String) {
    Home("HOME", "⌂"), Library("LIBRARY", "♫"), Profile("PROFILE", "☺")
}

/**
 * APP ROOT. Two ViewModels:
 *   explorer = data from the internet (home, search, playlists)
 *   player   = audio playback + your library + listening log
 *
 * RESPONSIVE: BoxWithConstraints tells us the available width.
 *   < 600dp  (phones)            : bottom navigation bar
 *   >= 600dp (tablets/landscape) : navigation rail on the left, content centred with a max width
 */
@Composable
fun MusicManiaApp(
    explorer: ExplorerViewModel = viewModel(),
    player: PlayerViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var query by rememberSaveable { mutableStateOf("") }
    val onQuery: (String) -> Unit = { query = it; explorer.onQuery(it) }
    val onTab: (Tab) -> Unit = { tab = it; onQuery("") }

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val wide = maxWidth >= 600.dp

        CompositionLocalProvider(LocalWide provides wide, LocalBottomInset provides (if (wide) 100.dp else 170.dp)) {
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        NavRail(tab, onTab)
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Content(tab, query, onQuery, explorer, player, Modifier.widthIn(max = 900.dp).fillMaxHeight())
                            MiniPlayerHost(player, Modifier.align(Alignment.BottomCenter).widthIn(max = 900.dp).padding(bottom = 8.dp))
                        }
                    }
                } else {
                    Content(tab, query, onQuery, explorer, player, Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.BottomCenter)) {
                        MiniPlayerHost(player)
                        NavBar(tab, onTab)
                    }
                }
            }
        }

        // Full player slides up over everything; the back button closes it.
        AnimatedVisibility(player.playerOpen, enter = slideInVertically(tween(500)) { it }, exit = slideOutVertically(tween(500)) { it }) {
            PlayerScreen(player)
        }

        // Toast (message comes from the player ViewModel)
        val message = player.toast
        LaunchedEffect(message) { if (message != null) { delay(1600); player.clearToast() } }
        AnimatedVisibility(message != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp).navigationBarsPadding(), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.pixelShadow(Color.Black).background(Cream).padding(horizontal = 14.dp, vertical = 9.dp)) {
                RText(message ?: "", color = Color(0xFF1A0F00), size = 10.sp, pixel = true)
            }
        }
    }
}

/** Header + search bar (on every screen) and the body: search results OR the selected tab. */
@Composable
private fun Content(tab: Tab, query: String, onQuery: (String) -> Unit, explorer: ExplorerViewModel, player: PlayerViewModel, modifier: Modifier) {
    Column(modifier) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(14.dp)) {      // tiny three-square pixel logo
                    Box(Modifier.size(7.dp).background(Pink))
                    Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(Cyan))
                    Box(Modifier.align(Alignment.BottomStart).size(7.dp).background(Green))
                }
                RText("MUSIC MANIA", Modifier.weight(1f), Amber, 20.sp, pixel = true)
                RText("SIDE A", color = Muted, size = 9.sp, pixel = true)
            }
            RetroSearchBar(query, onChange = onQuery)
        }
        Box(Modifier.weight(1f)) {
            Crossfade(tab, label = "tabs") { current ->
                when (current) {
                    Tab.Home -> HomeScreen(explorer, player)
                    Tab.Library -> LibraryScreen(explorer, player)
                    Tab.Profile -> ProfileScreen(player)
                }
            }
            if (query.isNotBlank()) SearchResults(explorer.search ?: UiState.Loading, player)
        }
    }
}

/** Live results from the API (debounced in the ViewModel). */
@Composable
private fun SearchResults(state: UiState<List<Song>>, player: PlayerViewModel) {
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = LocalBottomInset.current)) {
        when (state) {
            UiState.Loading -> LoadingBlock(rows = 4)
            is UiState.Error -> ErrorBox(state.message, onRetry = null)
            is UiState.Success -> if (state.data.isEmpty()) {
                SectionTitle("NO TAPES FOUND")
                RText("Try another song or artist.", color = Muted, size = 11.sp)
            } else {
                SectionTitle("RESULTS")
                state.data.forEachIndexed { i, song ->
                    SongRow(song, i + 1, player.current?.id == song.id, player.isLiked(song.id), { player.play(state.data, i) }, { player.toggleLike(song) })
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerHost(player: PlayerViewModel, modifier: Modifier = Modifier) {
    AnimatedVisibility(player.current != null && !player.playerOpen, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        val song = player.current ?: return@AnimatedVisibility
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                .pixelShadow(AmberDark).background(Panel2).border(2.dp, Amber)
                .clickable { player.openPlayer() }.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoverImage(song.coverUrl, song.hue, song.glyph, Modifier.size(38.dp), glyphSize = 14.sp)
            Column(Modifier.weight(1f)) {
                RText(song.title, size = 12.sp, maxLines = 1)
                RText(song.artist, color = Muted, size = 10.sp, maxLines = 1)
            }
            RText(if (player.isPlaying) "❚❚" else "▶\uFE0E", Modifier.clickable { player.togglePlay() }.padding(horizontal = 10.dp, vertical = 6.dp), Amber, 16.sp)
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.then(if (selected) Modifier.pixelShadow(AmberDark, 2.dp) else Modifier)
            .background(if (selected) Amber else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val c = if (selected) Color(0xFF1A0F00) else Muted
        RText(tab.icon, color = c, size = 16.sp)
        RText(tab.label, color = c, size = 10.sp, pixel = true)
    }
}

@Composable
private fun NavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .background(Panel.copy(alpha = .93f), RoundedCornerShape(6.dp)).border(2.dp, Color(0xFF31254A), RoundedCornerShape(6.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { Tab.entries.forEach { NavItem(it, it == selected, { onSelect(it) }, Modifier.weight(1f)) } }
}

/** Vertical navigation for wide screens. */
@Composable
private fun NavRail(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(92.dp).padding(8.dp)
            .background(Panel, RoundedCornerShape(6.dp)).border(2.dp, Color(0xFF31254A), RoundedCornerShape(6.dp)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { Tab.entries.forEach { NavItem(it, it == selected, { onSelect(it) }, Modifier.fillMaxWidth()) } }
}
