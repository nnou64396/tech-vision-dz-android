package com.techvisiondz.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radius scale from the production site: --radius-md 12px (buttons/inputs/chips),
// --radius-lg 16px (cards), --radius-xl 24px (hero surfaces), --radius-full pills.
object TechVisionRadii {
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Full = CircleShape
}

/**
 * Material 3 shape slots configured with the website's corner language so
 * Material components (buttons, cards, text fields, dialogs) inherit the brand
 * radii without ad-hoc `RoundedCornerShape` values sprinkled in screens.
 */
val TechVisionShapes = Shapes(
    extraSmall = RoundedCornerShape(TechVisionRadii.Sm),
    small = RoundedCornerShape(TechVisionRadii.Md),
    medium = RoundedCornerShape(TechVisionRadii.Lg),
    large = RoundedCornerShape(TechVisionRadii.Xl),
    extraLarge = RoundedCornerShape(28.dp),
)