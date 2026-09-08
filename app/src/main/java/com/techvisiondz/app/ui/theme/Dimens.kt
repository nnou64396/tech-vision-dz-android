package com.techvisiondz.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * TECH VISION DZ spacing + elevation tokens.
 *
 * Spacing follows the website scale (4/8/12/16/24/32/44/56/80/112 px) trimmed to
 * the Android-appropriate subset. Elevation is deliberately restrained
 * (border-first editorial surfaces) — the site's depth comes from borders and
 * subtle color, not heavy shadows.
 */
object TechVisionSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Section = 44.dp
}

object TechVisionElevation {
    val None = 0.dp
    val Flat = 1.dp
    val Card = 2.dp
    val Floating = 8.dp
}