package com.techvisiondz.app.core.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import kotlinx.coroutines.delay

/** Rendered mascot height used by the home brand header. */
private const val FennecMascotSizeDp = 40

/** The approved artwork's native width/height ratio (540x639). */
private const val FennecAspectRatio = 540f / 639f

/** Default mascot height (matches the website's header mascot proportion). */
val FennecMascotSize: Dp = FennecMascotSizeDp.dp

/**
 * TECH VISION DZ fennec mascot — the approved website mascot rendered as a
 * native Android VectorDrawable (path data preserved verbatim from
 * `fennec-final-optimized.svg`, transparent background, native 540x639 ratio).
 *
 * The artwork itself is never modified here; every animation is a pure
 * transform (offset, rotation, alpha) applied on the GPU so the mascot's
 * silhouette never changes.
 *
 * Animation mirrors the website's mascot: a one-shot entrance "peek", a slow
 * idle sway and an occasional perk. All movement is optional decoration and
 * respects the system "remove animations" preference
 * (android.provider.Settings.Global.ANIMATOR_DURATION_SCALE): when animations
 * are disabled the mascot is shown statically and never moves.
 */
@Composable
fun FennecMascot(
    modifier: Modifier = Modifier,
    height: Dp = FennecMascotSize,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    // One-shot entrance: opacity 0 -> 1 with a slight rise into place, delayed
    // briefly so it lands right after the header appears.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            entrance.snapTo(1f)
        } else {
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1100,
                    delayMillis = 250,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    // Idle sway + occasional perk are only driven when motion is allowed.
    val idleRot: Float
    val idleLift: Float
    val perk = remember { Animatable(0f) }
    if (reducedMotion) {
        idleRot = 0f
        idleLift = 0f
    } else {
        val idle = rememberInfiniteTransition(label = "fennecIdle")
        val rot by idle.animateFloat(
            initialValue = 0f,
            targetValue = -1.2f,
            animationSpec = infiniteRepeatable(
                // Mirrors the website's 4.8s idle keyframes so the loop ends
                // where it starts (no snap).
                animation = keyframes {
                    durationMillis = 4800
                    0f at 0 with LinearEasing
                    -1.2f at 1584 with FastOutSlowInEasing
                    0.8f at 3168 with LinearOutSlowInEasing
                    0f at 4800 with FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "fennecIdleRot",
        )
        val lift by idle.animateFloat(
            initialValue = 0f,
            targetValue = -2.2f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4800
                    0f at 0 with LinearEasing
                    -2.2f at 1584 with FastOutSlowInEasing
                    -1.1f at 3168 with LinearOutSlowInEasing
                    0f at 4800 with FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "fennecIdleLift",
        )
        idleRot = rot
        idleLift = lift
        LaunchedEffect(Unit) {
            // First perk shortly after the entrance settles, then an occasional
            // one following the website's 11s cadence.
            delay(1600)
            while (true) {
                perk.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                delay(180)
                perk.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
                delay(11000)
            }
        }
    }

    Image(
        painter = painterResource(R.drawable.fennec_mascot),
        contentDescription = contentDescription,
        modifier = modifier
            // Preserve the artwork's native 540x639 ratio at the requested size.
            .size(width = height * FennecAspectRatio, height = height)
            .graphicsLayer {
                val entranceValue = entrance.value
                alpha = entranceValue
                translationY =
                    (1f - entranceValue) * 12.dp.toPx() + idleLift.dp.toPx() -
                        perk.value * 3.dp.toPx()
                rotationZ = idleRot + perk.value * 1.6f
            },
        contentScale = ContentScale.Fit,
    )
}