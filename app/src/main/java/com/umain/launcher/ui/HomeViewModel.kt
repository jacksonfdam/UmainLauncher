package com.umain.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.umain.launcher.data.AppInfo
import com.umain.launcher.data.AppRepository
import com.umain.launcher.data.LauncherPreferences
import com.umain.launcher.data.PackageDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the launcher's state: the installed apps plus the user's hidden/favorite
 * choices (persisted via [LauncherPreferences]). All mutations are fire-and-forget
 * into DataStore; the exposed [StateFlow]s update reactively.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val preferences = LauncherPreferences(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    /** Every launchable app (including hidden ones — the drawer filters those). */
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    val hiddenPackages: StateFlow<Set<String>> = preferences.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val favoritePackages: StateFlow<Set<String>> = preferences.favoriteApps
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Favorites resolved to [AppInfo] and kept in pin order, for the home dock. */
    val favoriteApps: StateFlow<List<AppInfo>> =
        combine(_apps, preferences.favoriteApps) { apps, favorites ->
            favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _apps.value = repository.loadApps() }
    }

    fun launch(packageName: String) = repository.launchApp(packageName)

    fun openAppInfo(packageName: String) = repository.openAppInfo(packageName)

    fun requestUninstall(packageName: String) = repository.requestUninstall(packageName)

    fun requestUninstall(packageNames: Collection<String>) =
        packageNames.forEach(repository::requestUninstall)

    fun setHidden(packageNames: Collection<String>, hidden: Boolean) {
        viewModelScope.launch { preferences.setHidden(packageNames, hidden) }
    }

    fun setHidden(packageName: String, hidden: Boolean) =
        setHidden(listOf(packageName), hidden)

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch { preferences.toggleFavorite(packageName) }
    }

    fun launchActivity(packageName: String, className: String): Boolean =
        repository.launchActivity(packageName, className)

    fun openSettings(action: String): Boolean = repository.openSettings(action)

    suspend fun inspectPackage(packageName: String): PackageDetails? =
        repository.inspectPackage(packageName)
}
