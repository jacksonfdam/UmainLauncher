package com.umain.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umain.launcher.ui.theme.UmainLauncherTheme

/**
 * Root composable: applies the user's theme, provides the active icon color filter,
 * then hosts [LauncherRoot]. Kept separate from [LauncherRoot] so the theme reacts
 * to settings changes.
 */
@Composable
fun LauncherApp(viewModel: HomeViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    UmainLauncherTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        colorTheme = settings.colorTheme,
    ) {
        CompositionLocalProvider(
            LocalIconFilter provides settings.iconFilter,
            LocalIconShape provides settings.iconShape,
        ) {
            LauncherRoot(viewModel = viewModel, settings = settings)
        }
    }
}
