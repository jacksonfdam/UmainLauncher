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
            themeMode = p[THEME_MODE].toEnum(ThemeMode.SYSTEM),
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
        )
    }

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

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        val HIDDEN = stringSetPreferencesKey("hidden_apps")
        val FAVORITES = stringPreferencesKey("favorite_apps")
        val LAYOUT = stringPreferencesKey("layout_mode")
        val COLUMNS = intPreferencesKey("grid_columns")
        val ICON_SIZE = stringPreferencesKey("icon_size")
        val ICON_FILTER = stringPreferencesKey("icon_filter")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        const val SEPARATOR = "\n"
    }
}
