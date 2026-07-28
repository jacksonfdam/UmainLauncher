package com.umain.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.umain.launcher.data.AppInfo
import com.umain.launcher.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the launcher's UI state: the list of installed apps. Survives
 * configuration changes so we don't re-query the PackageManager on every rotate.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    init {
        refresh()
    }

    /** Reloads the app list — call this when apps are installed or removed. */
    fun refresh() {
        viewModelScope.launch {
            _apps.value = repository.loadApps()
        }
    }

    fun launch(packageName: String) = repository.launchApp(packageName)
}
