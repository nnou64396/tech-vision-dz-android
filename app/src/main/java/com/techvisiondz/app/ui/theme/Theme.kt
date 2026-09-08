package com.techvisiondz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Dynamic color (Material You) intentionally defaults to OFF so the TECH VISION
// DZ brand identity stays consistent across devices. Can be enabled later if a
// Material-You experience is desired.
@Composable
fun TechVisionDzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> TechVisionDarkColorScheme
        else -> TechVisionLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TechVisionTypography,
        shapes = TechVisionShapes,
        content = content,
    )
}