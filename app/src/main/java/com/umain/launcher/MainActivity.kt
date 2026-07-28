package com.umain.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.umain.launcher.ui.HomeViewModel
import com.umain.launcher.ui.LauncherRoot
import com.umain.launcher.ui.theme.UmainLauncherTheme

/**
 * The single Activity that backs the launcher. Because the manifest declares
 * `singleTask` + the HOME intent filter, the system keeps one instance alive and
 * routes the Home button back to it.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UmainLauncherTheme {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-read the app list when we come back — an app may have been
        // installed or uninstalled while we were away.
        viewModel.refresh()
    }
}
