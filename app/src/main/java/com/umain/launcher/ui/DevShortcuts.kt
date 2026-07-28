package com.umain.launcher.ui

import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A dev-focused quick shortcut that opens a system Settings screen. These use only
 * public `Settings.ACTION_*` intents; unavailable ones fail gracefully (see
 * [com.umain.launcher.data.AppRepository.openSettings]).
 */
data class DevShortcut(
    val label: String,
    val icon: ImageVector,
    val action: String,
)

val DEV_SHORTCUTS: List<DevShortcut> = listOf(
    DevShortcut(
        label = "Dev options",
        icon = Icons.Rounded.DeveloperMode,
        action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
    ),
    DevShortcut(
        label = "Device info",
        icon = Icons.Rounded.PhoneAndroid,
        action = Settings.ACTION_DEVICE_INFO_SETTINGS,
    ),
    DevShortcut(
        label = "Settings",
        icon = Icons.Rounded.Settings,
        action = Settings.ACTION_SETTINGS,
    ),
)
