package com.techvisiondz.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.navigation.AppNavHost
import com.techvisiondz.app.feature.update.UpdateDialogHost
import com.techvisiondz.app.feature.update.UpdateViewModel
import com.techvisiondz.app.ui.theme.TechVisionDzTheme

/**
 * Root composable of the app.
 * Wires the TECH VISION DZ theme around the navigation graph, and hosts the
 * global update dialog.
 *
 * Startup update check: a single, non-blocking automatic check runs once per
 * app session (keyed on [Unit], so recompositions never re-trigger it). It is
 * gated by [AppConfig.UPDATER_ENABLED], honours the 24h "Later" deferral
 * cooldown via [UpdateViewModel.checkForUpdate] (manual = false), and never
 * blocks first render — it only ever surfaces a dialog when an update is
 * actually available. If the check fails, the app keeps working normally.
 */
@Composable
fun TechVisionDzApp() {
    TechVisionDzTheme {
        val updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory)

        LaunchedEffect(Unit) {
            if (AppConfig.UPDATER_ENABLED) {
                updateViewModel.checkForUpdate()
            }
        }

        AppNavHost(updateViewModel = updateViewModel)

        UpdateDialogHost(updateViewModel = updateViewModel)
    }
}