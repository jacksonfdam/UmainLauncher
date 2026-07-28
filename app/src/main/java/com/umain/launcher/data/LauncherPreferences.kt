package com.umain.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

/**
 * Persists the user's launcher choices: which apps are hidden from the drawer and
 * which are pinned to the home dock. Backed by Preferences DataStore.
 */
class LauncherPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    /** Package names the user has chosen to hide from the drawer. */
    val hiddenApps: Flow<Set<String>> = store.data.map { it[HIDDEN] ?: emptySet() }

    /** Pinned packages, in the order they were added (dock order). */
    val favoriteApps: Flow<List<String>> = store.data.map { prefs ->
        prefs[FAVORITES]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        store.edit { prefs ->
            val current = prefs[HIDDEN] ?: emptySet()
            prefs[HIDDEN] = if (hidden) current + packageName else current - packageName
        }
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

    /** Drops a package from both sets — call after an app is uninstalled. */
    suspend fun forget(packageName: String) {
        store.edit { prefs ->
            prefs[HIDDEN] = (prefs[HIDDEN] ?: emptySet()) - packageName
            val favs = prefs[FAVORITES]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
            prefs[FAVORITES] = (favs - packageName).joinToString(SEPARATOR)
        }
    }

    private companion object {
        val HIDDEN = stringSetPreferencesKey("hidden_apps")
        val FAVORITES = stringPreferencesKey("favorite_apps")
        const val SEPARATOR = "\n"
    }
}
