package com.musicmania.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ---- Palette: plum-black base with warm amber + neon accents ----
val Bg = Color(0xFF0F0B16)
val Panel = Color(0xFF1A1424)
val Panel2 = Color(0xFF251C33)
val Amber = Color(0xFFFFB03A)
val AmberDark = Color(0xFF7A4A00)   // used as the hard "pixel shadow" under amber items
val Green = Color(0xFF5FE36B)
val Pink = Color(0xFFFF4F81)
val Cyan = Color(0xFF35D0FF)
val Cream = Color(0xFFF3E9D2)
val Muted = Color(0xFF8D82A3)
val Line = Color(0xFF3A2D52)

// ---- Fonts ----
// To get the exact web look: download "Silkscreen" and "Space Mono" (Google Fonts),
// put the .ttf files in app/src/main/res/font/ (lowercase names, e.g. silkscreen_bold.ttf),
// then replace these two lines with:
//   val PixelFont = FontFamily(Font(R.font.silkscreen_bold, FontWeight.Bold))
//   val BodyFont  = FontFamily(Font(R.font.space_mono))
val PixelFont: FontFamily = FontFamily.Monospace
val BodyFont: FontFamily = FontFamily.Monospace
