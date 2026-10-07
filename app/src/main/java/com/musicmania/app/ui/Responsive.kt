package com.musicmania.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout info shared with every screen (set once in MusicManiaApp).
 * CompositionLocals avoid passing the same parameter through every function.
 */
val LocalWide = compositionLocalOf { false }                 // true on tablets / landscape (width >= 600dp)
val LocalBottomInset = compositionLocalOf<Dp> { 170.dp }     // space screens leave for the mini-player + nav bar
