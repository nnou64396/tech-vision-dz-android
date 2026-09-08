package com.techvisiondz.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// TECH VISION DZ brand palette.
//
// Extracted from the production website (read-only audit of `tech-vision-dz`):
//   dark  -> bg #06080F · surface #0F1320 · border #1C2438 / #263050
//            text #D0D6E4 · heading #EEF1F7 · muted #7D8AA4
//            primary #0090FF (electric blue) · accent #00D4AA (teal)
//            violet #8B5CF6 · danger #F43F5E
//   light -> bg #F0F4F8 · surface #FFFFFF · border #D4DCE8 / #B8C5D6
//            text #1A2332 · heading #0A1628 · muted #5A6B82
//            primary #0066CC · accent #009688
// The site ships a dark-first identity; both themes live here as one source of
// truth and are swapped through [lightColorScheme] / [darkColorScheme].
// ---------------------------------------------------------------------------

private object TechVisionDarkColors {
    val Primary = Color(0xFF33A5FF)
    val OnPrimary = Color(0xFF00264B)
    val PrimaryContainer = Color(0xFF0E2B4E)
    val OnPrimaryContainer = Color(0xFFB8DBFF)

    val Secondary = Color(0xFF00C7A1)
    val OnSecondary = Color(0xFF003829)
    val SecondaryContainer = Color(0xFF063A31)
    val OnSecondaryContainer = Color(0xFF9FF2DE)

    val Tertiary = Color(0xFFA78BFA)
    val OnTertiary = Color(0xFF2A1448)
    val TertiaryContainer = Color(0xFF3B2B6B)
    val OnTertiaryContainer = Color(0xFFE4DBFF)

    val Background = Color(0xFF06080F)
    val OnBackground = Color(0xFFD0D6E4)
    val Surface = Color(0xFF0F1320)
    val OnSurface = Color(0xFFEEF1F7)
    val SurfaceVariant = Color(0xFF111625)
    val OnSurfaceVariant = Color(0xFF9AA6BD)

    val SurfaceContainerLowest = Color(0xFF06080F)
    val SurfaceContainerLow = Color(0xFF0A0E1A)
    val SurfaceContainer = Color(0xFF0F1320)
    val SurfaceContainerHigh = Color(0xFF151B2E)
    val SurfaceContainerHighest = Color(0xFF1C2438)

    val Outline = Color(0xFF263050)
    val OutlineVariant = Color(0xFF1C2438)

    val Error = Color(0xFFF43F5E)
    val OnError = Color(0xFF3B0710)
    val ErrorContainer = Color(0xFF5C1420)
    val OnErrorContainer = Color(0xFFFFD9DD)
}

private object TechVisionLightColors {
    val Primary = Color(0xFF0066CC)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFDBEAFE)
    val OnPrimaryContainer = Color(0xFF0A1628)

    val Secondary = Color(0xFF007266)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFCCF1E9)
    val OnSecondaryContainer = Color(0xFF0A1628)

    val Tertiary = Color(0xFF6D4DBE)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFEAE3FB)
    val OnTertiaryContainer = Color(0xFF1B1233)

    val Background = Color(0xFFF0F4F8)
    val OnBackground = Color(0xFF1A2332)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1A2332)
    val SurfaceVariant = Color(0xFFEEF2F7)
    val OnSurfaceVariant = Color(0xFF5A6B82)

    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF0F4F8)
    val SurfaceContainer = Color(0xFFFFFFFF)
    val SurfaceContainerHigh = Color(0xFFE4EAF2)
    val SurfaceContainerHighest = Color(0xFFD4DCE8)

    val Outline = Color(0xFFA9B6C8)
    val OutlineVariant = Color(0xFFD4DCE8)

    val Error = Color(0xFFDC2626)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFDE7E7)
    val OnErrorContainer = Color(0xFF3B0710)
}

internal val TechVisionLightColorScheme = lightColorScheme(
    primary = TechVisionLightColors.Primary,
    onPrimary = TechVisionLightColors.OnPrimary,
    primaryContainer = TechVisionLightColors.PrimaryContainer,
    onPrimaryContainer = TechVisionLightColors.OnPrimaryContainer,
    secondary = TechVisionLightColors.Secondary,
    onSecondary = TechVisionLightColors.OnSecondary,
    secondaryContainer = TechVisionLightColors.SecondaryContainer,
    onSecondaryContainer = TechVisionLightColors.OnSecondaryContainer,
    tertiary = TechVisionLightColors.Tertiary,
    onTertiary = TechVisionLightColors.OnTertiary,
    tertiaryContainer = TechVisionLightColors.TertiaryContainer,
    onTertiaryContainer = TechVisionLightColors.OnTertiaryContainer,
    background = TechVisionLightColors.Background,
    onBackground = TechVisionLightColors.OnBackground,
    surface = TechVisionLightColors.Surface,
    onSurface = TechVisionLightColors.OnSurface,
    surfaceVariant = TechVisionLightColors.SurfaceVariant,
    onSurfaceVariant = TechVisionLightColors.OnSurfaceVariant,
    surfaceContainerLowest = TechVisionLightColors.SurfaceContainerLowest,
    surfaceContainerLow = TechVisionLightColors.SurfaceContainerLow,
    surfaceContainer = TechVisionLightColors.SurfaceContainer,
    surfaceContainerHigh = TechVisionLightColors.SurfaceContainerHigh,
    surfaceContainerHighest = TechVisionLightColors.SurfaceContainerHighest,
    outline = TechVisionLightColors.Outline,
    outlineVariant = TechVisionLightColors.OutlineVariant,
    error = TechVisionLightColors.Error,
    onError = TechVisionLightColors.OnError,
    errorContainer = TechVisionLightColors.ErrorContainer,
    onErrorContainer = TechVisionLightColors.OnErrorContainer,
)

internal val TechVisionDarkColorScheme = darkColorScheme(
    primary = TechVisionDarkColors.Primary,
    onPrimary = TechVisionDarkColors.OnPrimary,
    primaryContainer = TechVisionDarkColors.PrimaryContainer,
    onPrimaryContainer = TechVisionDarkColors.OnPrimaryContainer,
    secondary = TechVisionDarkColors.Secondary,
    onSecondary = TechVisionDarkColors.OnSecondary,
    secondaryContainer = TechVisionDarkColors.SecondaryContainer,
    onSecondaryContainer = TechVisionDarkColors.OnSecondaryContainer,
    tertiary = TechVisionDarkColors.Tertiary,
    onTertiary = TechVisionDarkColors.OnTertiary,
    tertiaryContainer = TechVisionDarkColors.TertiaryContainer,
    onTertiaryContainer = TechVisionDarkColors.OnTertiaryContainer,
    background = TechVisionDarkColors.Background,
    onBackground = TechVisionDarkColors.OnBackground,
    surface = TechVisionDarkColors.Surface,
    onSurface = TechVisionDarkColors.OnSurface,
    surfaceVariant = TechVisionDarkColors.SurfaceVariant,
    onSurfaceVariant = TechVisionDarkColors.OnSurfaceVariant,
    surfaceContainerLowest = TechVisionDarkColors.SurfaceContainerLowest,
    surfaceContainerLow = TechVisionDarkColors.SurfaceContainerLow,
    surfaceContainer = TechVisionDarkColors.SurfaceContainer,
    surfaceContainerHigh = TechVisionDarkColors.SurfaceContainerHigh,
    surfaceContainerHighest = TechVisionDarkColors.SurfaceContainerHighest,
    outline = TechVisionDarkColors.Outline,
    outlineVariant = TechVisionDarkColors.OutlineVariant,
    error = TechVisionDarkColors.Error,
    onError = TechVisionDarkColors.OnError,
    errorContainer = TechVisionDarkColors.ErrorContainer,
    onErrorContainer = TechVisionDarkColors.OnErrorContainer,
)

/**
 * Brand "wave" motion — blue → teal — mirroring the website's primary gradient
 * system (used on primary buttons, active chips, section accents and the brand
 * mark). Gray end for the button stop is derived from the active color scheme.
 */
fun androidx.compose.material3.ColorScheme.brandGradient() =
    Brush.linearGradient(colors = listOf(primary, secondary))