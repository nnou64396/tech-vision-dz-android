package com.techvisiondz.app.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.techvisiondz.app.core.settings.AppLanguage
import java.util.Locale

/**
 * Applies the selected UI language to the whole subtree **without** recreating
 * the activity or the navigation stack.
 *
 * The app's strings resolve through resources, so [LocalContext] is swapped
 * for a configuration-scoped context whose locale matches [language]; every
 * [androidx.compose.ui.res.stringResource] below sees that localisation. The
 * layout direction follows the language (RTL for Arabic, LTR for English) via
 * [LocalLayoutDirection], so Arabic stays right-to-left and English
 * left-to-right even when the device locale differs.
 *
 * Both locals are pure composition data: no activity finish/start, no saved
 * state loss, and existing ViewModels / navigation survive the switch.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val localizedContext = remember(context, language) {
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language.code))
            },
        )
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides
            if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}