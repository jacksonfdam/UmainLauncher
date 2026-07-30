package com.umain.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.umain.launcher.ui.AppWidgetHostController
import com.umain.launcher.ui.HomeViewModel
import com.umain.launcher.ui.LauncherApp
import com.umain.launcher.ui.LocalAppWidgetHost

/**
 * The single Activity that backs the launcher. Because the manifest declares
 * `singleTask` + the HOME intent filter, the system keeps one instance alive and
 * routes the Home button back to it. It also owns the [AppWidgetHostController] and
 * drives its listening lifecycle.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var appWidgetHost: AppWidgetHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetHost = AppWidgetHostController(this)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppWidgetHost provides appWidgetHost) {
                LauncherApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // An app may have been installed/uninstalled while we were away.
        viewModel.refresh()
    }
}
