package com.umain.launcher.data

/** Number of columns allowed in the app drawer grid. */
val COLUMN_RANGE = 3..6

/** Image-style color filter applied to every app icon. */
enum class IconFilter { NONE, GRAYSCALE, DESATURATED, SEPIA }

/** Icon size buckets for the drawer grid (mapped to dp in the UI layer). */
enum class IconSize { SMALL, MEDIUM, LARGE }

/** App theme selection. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Drawer layout. [GRID] is the icon grid; [MINIMAL] is a text-only "Minimal AF"
 * list — app names, no icons, no chrome.
 */
enum class LayoutMode { GRID, MINIMAL }

/**
 * User-configurable launcher settings, persisted via [LauncherPreferences].
 * Kept Compose-free so it lives in the data layer; the UI layer maps the enums
 * to dp / ColorFilter / color schemes.
 */
data class LauncherSettings(
    val layout: LayoutMode = LayoutMode.GRID,
    val columns: Int = 4,
    val iconSize: IconSize = IconSize.MEDIUM,
    val iconFilter: IconFilter = IconFilter.NONE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)
