package com.techvisiondz.app.feature.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.techvisiondz.app.core.ui.components.LoadingState

/**
 * Full-screen branded loading state shown while the persisted session is being
 * restored. The app can't decide between the auth flow and the main app until
 * [com.techvisiondz.app.core.data.AuthState] leaves [com.techvisiondz.app.core.data.AuthState.Loading].
 */
@Composable
fun AuthLoadingScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LoadingState(modifier = Modifier.fillMaxSize().padding(innerPadding))
    }
}