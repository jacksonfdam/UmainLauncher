package com.umain.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

/**
 * Persists the user's launcher choices: hidden apps, the favorites dock, and the
 * appearance settings (grid, icon filter, theme). Backed by Preferences DataStore.
 */
class LauncherPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    /** Package names the user has chosen to hide from the drawer. */
    val hiddenApps: Flow<Set<String>> = store.data.map { it[HIDDEN] ?: emptySet() }

    /** Pinned packages, in the order they were added (dock order). */
    val favoriteApps: Flow<List<String>> = store.data.map { prefs ->
        prefs[FAVORITES]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    /** All appearance settings as one object. */
    val settings: Flow<LauncherSettings> = store.data.map { p ->
        LauncherSettings(
            layout = p[LAYOUT].toEnum(LayoutMode.GRID),
            columns = (p[COLUMNS] ?: 4).coerceIn(COLUMN_RANGE.first, COLUMN_RANGE.last),
            iconSize = p[ICON_SIZE].toEnum(IconSize.MEDIUM),
            iconFilter = p[ICON_FILTER].toEnum(IconFilter.NONE),
            iconShape = p[ICON_SHAPE].toEnum(IconShape.SYSTEM),
            themeMode = p[THEME_MODE].toEnum(ThemeMode.SYSTEM),
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
            colorTheme = p[COLOR_THEME].toEnum(ColorTheme.PURPLE),
            showStatusWidget = p[SHOW_WIDGET] ?: true,
        )
    }

    /** Per-widget home placement (position + scale), keyed by [WidgetIds]. */
    val widgetLayout: Flow<Map<String, WidgetPlacement>> =
        store.data.map { parseLayout(it[WIDGET_LAYOUT]) }

    suspend fun setHidden(packageNames: Collection<String>, hidden: Boolean) {
        store.edit { prefs ->
            val current = prefs[HIDDEN] ?: emptySet()
            prefs[HIDDEN] = if (hidden) current + packageNames else current - packageNames.toSet()
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        store.edit { prefs ->
            val current = prefs[FAVORITES]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = if (packageName in current) current - packageName else current + packageName
            prefs[FAVORITES] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun setLayout(mode: LayoutMode) = store.edit { it[LAYOUT] = mode.name }

    suspend fun setColumns(columns: Int) =
        store.edit { it[COLUMNS] = columns.coerceIn(COLUMN_RANGE.first, COLUMN_RANGE.last) }

    suspend fun setIconSize(size: IconSize) = store.edit { it[ICON_SIZE] = size.name }

    suspend fun setIconFilter(filter: IconFilter) = store.edit { it[ICON_FILTER] = filter.name }

    suspend fun setIconShape(shape: IconShape) = store.edit { it[ICON_SHAPE] = shape.name }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }

    suspend fun setColorTheme(theme: ColorTheme) = store.edit { it[COLOR_THEME] = theme.name }

    suspend fun setShowStatusWidget(show: Boolean) = store.edit { it[SHOW_WIDGET] = show }

    suspend fun setWidgetPlacement(id: String, placement: WidgetPlacement) = store.edit {
        val current = parseLayout(it[WIDGET_LAYOUT]).toMutableMap()
        current[id] = placement
        it[WIDGET_LAYOUT] = formatLayout(current)
    }

    suspend fun resetWidgetLayout() = store.edit { it.remove(WIDGET_LAYOUT) }

    private fun parseLayout(raw: String?): Map<String, WidgetPlacement> =
        raw?.split(";").orEmpty().mapNotNull { entry ->
            runCatching {
                val (id, rest) = entry.split(":", limit = 2)
                val (dx, dy, scale) = rest.split(",")
                id to WidgetPlacement(dx.toFloat(), dy.toFloat(), scale.toFloat())
            }.getOrNull()
        }.toMap()

    private fun formatLayout(map: Map<String, WidgetPlacement>): String =
        map.entries.joinToString(";") { (id, p) -> "$id:${p.dx},${p.dy},${p.scale}" }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        val HIDDEN = stringSetPreferencesKey("hidden_apps")
        val FAVORITES = stringPreferencesKey("favorite_apps")
        val LAYOUT = stringPreferencesKey("layout_mode")
        val COLUMNS = intPreferencesKey("grid_columns")
        val ICON_SIZE = stringPreferencesKey("icon_size")
        val ICON_FILTER = stringPreferencesKey("icon_filter")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val SHOW_WIDGET = booleanPreferencesKey("show_status_widget")
        val WIDGET_LAYOUT = stringPreferencesKey("widget_layout")
        const val SEPARATOR = "\n"
    }
}
