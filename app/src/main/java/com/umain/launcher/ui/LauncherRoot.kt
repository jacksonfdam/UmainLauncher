package com.umain.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Top-level launcher UI. The home screen is always present; the app drawer
 * slides up over it and can be dismissed with the system back gesture.
 */
@Composable
fun LauncherRoot(viewModel: HomeViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var drawerOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(onOpenDrawer = { drawerOpen = true })

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            AppDrawer(
                apps = apps,
                onAppClick = { app ->
                    viewModel.launch(app.packageName)
                    drawerOpen = false
                },
            )
        }
    }

    // Back closes the drawer instead of leaving the launcher.
    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }
}
