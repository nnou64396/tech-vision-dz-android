package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import com.techvisiondz.app.ui.theme.brandGradient

// The site's gradient stops (#0090FF → #00D4AA in dark, #0066CC → #009688 in
// light) are bright enough that a deep navy label keeps WCAG contrast while
// keeping the electric "wave" identity.
private val BrandOnGradient = Color(0xFF06121F)

/**
 * Official TECH VISION DZ logo — the exact asset the website brand (header and
 * footer) uses, kept verbatim. The website swaps the artwork by theme
 * (`logo.png` for light, `logo-dark.png` for the default dark); the app mirrors
 * that here. `size` is the rendered height; the width follows the logo's native
 * ~2:1 ratio so it is never stretched or cropped. The logo carries no
 * background of its own and none is added.
 *
 * @param contentDescription null by default (decorative) so the logo does not
 *   re-announce the brand next to surrounding brand text; pass a label where it
 *   appears standalone (e.g. the empty state).
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    contentDescription: String? = null,
) {
    val logo = if (isSystemInDarkTheme()) {
        painterResource(R.drawable.brand_logo_dark)
    } else {
        painterResource(R.drawable.brand_logo_light)
    }
    Image(
        painter = logo,
        contentDescription = contentDescription,
        modifier = modifier.height(size),
        contentScale = ContentScale.FillHeight,
    )
}

/** Primary CTA with the brand blue→teal gradient background. */
@Composable
fun TechGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TechVisionRadii.Md),
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = MaterialTheme.colorScheme.brandGradient(),
                    shape = RoundedCornerShape(TechVisionRadii.Md),
                )
                .padding(horizontal = TechVisionSpacing.Xl, vertical = TechVisionSpacing.Md),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = BrandOnGradient, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Editorial section heading: a short gradient accent bar + bold title. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = TechVisionSpacing.Lg)
                .clip(RoundedCornerShape(TechVisionRadii.Sm))
                .background(MaterialTheme.colorScheme.brandGradient()),
        )
        Spacer(modifier = Modifier.width(TechVisionSpacing.Sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Pill category label matching the website's category chips. */
@Composable
fun CategoryChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = TechVisionRadii.Full,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = TechVisionSpacing.Md, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Compact muted metadata row. Every part is its own semantics element so
 * screen readers announce each item and tests can target a single field
 * (e.g. the author name) deterministically.
 */
@Composable
fun DottedMetaRow(
    parts: List<String>,
    modifier: Modifier = Modifier,
) {
    if (parts.isEmpty()) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                Text(text = "·", style = MaterialTheme.typography.labelMedium, color = color)
            }
            Text(
                text = part,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Brand mark + centered message used by the branded empty state. */
@Composable
fun CenterBrandMark(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark(size = 52.dp, contentDescription = stringResource(R.string.app_name))
        Spacer(modifier = Modifier.size(TechVisionSpacing.Lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}