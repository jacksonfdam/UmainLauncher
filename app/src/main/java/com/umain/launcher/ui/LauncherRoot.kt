package com.umain.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umain.launcher.data.AppInfo
import com.umain.launcher.data.LauncherSettings
import kotlinx.coroutines.launch

/**
 * Top-level launcher UI. The home screen (clock + favorites dock) is always present;
 * the app drawer slides up over it, and the Activity Launcher / Settings open over
 * everything. System Back peels back one layer at a time.
 *
 * Uninstalls are confirmed in-app and then run **sequentially** — one system uninstall
 * dialog at a time — since firing several at once makes Android drop all but one.
 */
@Suppress("DEPRECATION") // ACTION_UNINSTALL_PACKAGE returns a result we use to sequence.
@Composable
fun LauncherRoot(viewModel: HomeViewModel, settings: LauncherSettings) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val hidden by viewModel.hiddenPackages.collectAsStateWithLifecycle()
    val favoritePackages by viewModel.favoritePackages.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()

    var drawerOpen by remember { mutableStateOf(false) }
    var inspecting by remember { mutableStateOf<AppInfo?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Uninstall flow: packages awaiting the confirm dialog, then the active queue.
    var pendingUninstall by remember { mutableStateOf<List<String>>(emptyList()) }
    var uninstallQueue by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Whatever the user chose, move on to the next queued app and refresh.
        uninstallQueue = uninstallQueue.drop(1)
        viewModel.refresh()
    }

    LaunchedEffect(uninstallQueue) {
        val pkg = uninstallQueue.firstOrNull() ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", pkg, null))
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
        val launched = runCatching { uninstallLauncher.launch(intent) }.isSuccess
        if (!launched) uninstallQueue = uninstallQueue.drop(1) // skip and continue
    }

    val copyPackage: (AppInfo) -> Unit = { app ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("package", app.packageName))
        Toast.makeText(context, "Copied ${app.packageName}", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            favorites = favoriteApps,
            onOpenDrawer = { drawerOpen = true },
            onOpenSettings = { showSettings = true },
            onLaunchFavorite = { viewModel.launch(it.packageName) },
            onUnpinFavorite = { viewModel.toggleFavorite(it.packageName) },
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            AppDrawer(
                apps = apps,
                hiddenPackages = hidden,
                favoritePackages = favoritePackages,
                onLaunch = { viewModel.launch(it.packageName); drawerOpen = false },
                onOpenAppInfo = { viewModel.openAppInfo(it.packageName) },
                onCopyPackage = copyPackage,
                onInspect = { inspecting = it },
                onToggleFavorite = { viewModel.toggleFavorite(it.packageName) },
                onSetHidden = { pkgs, hide -> viewModel.setHidden(pkgs, hide) },
                onUninstall = { pendingUninstall = it.toList() },
                onDevShortcut = { action -> viewModel.openSettings(action) },
                onOpenSettings = { showSettings = true },
                layout = settings.layout,
                columns = settings.columns,
                iconSize = settings.iconSize.dp,
            )
        }

        AnimatedVisibility(
            visible = inspecting != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
        ) {
            inspecting?.let { app ->
                ActivityLauncherScreen(
                    app = app,
                    loadDetails = { viewModel.inspectPackage(it) },
                    onLaunchActivity = { pkg, cls -> viewModel.launchActivity(pkg, cls) },
                    onBack = { inspecting = null },
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
        ) {
            SettingsScreen(
                settings = settings,
                onLayout = viewModel::setLayout,
                onColumns = viewModel::setColumns,
                onIconSize = viewModel::setIconSize,
                onIconFilter = viewModel::setIconFilter,
                onThemeMode = viewModel::setThemeMode,
                onDynamicColor = viewModel::setDynamicColor,
                onOpenSystemWallpaper = viewModel::openWallpaperPicker,
                onApplyWallpaper = { uri, which ->
                    scope.launch {
                        val ok = viewModel.applyWallpaper(uri, which)
                        Toast.makeText(
                            context,
                            if (ok) "Wallpaper applied" else "Couldn't apply wallpaper",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                onBack = { showSettings = false },
            )
        }
    }

    if (pendingUninstall.isNotEmpty()) {
        val names = pendingUninstall.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg }?.label }
        AlertDialog(
            onDismissRequest = { pendingUninstall = emptyList() },
            title = {
                Text(if (pendingUninstall.size == 1) "Uninstall app?" else "Uninstall ${pendingUninstall.size} apps?")
            },
            text = { Text(names.joinToString(", ").ifBlank { "Selected apps" }) },
            confirmButton = {
                TextButton(onClick = {
                    uninstallQueue = pendingUninstall
                    pendingUninstall = emptyList()
                }) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = emptyList() }) { Text("Cancel") }
            },
        )
    }

    // Back peels one layer at a time: settings → inspector → drawer → (stay on home).
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = !showSettings && inspecting != null) { inspecting = null }
    BackHandler(enabled = !showSettings && inspecting == null && drawerOpen) { drawerOpen = false }
}
