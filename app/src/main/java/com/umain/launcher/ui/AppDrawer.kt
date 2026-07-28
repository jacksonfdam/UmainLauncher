package com.umain.launcher.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umain.launcher.data.AppInfo

/**
 * Full-screen, searchable grid of installed apps. Supports:
 *  - search by label **or** package name (dev-friendly),
 *  - long-press → per-app [AppActionSheet],
 *  - multi-select mode to hide or uninstall several apps at once,
 *  - a "show hidden" toggle, and a row of dev shortcuts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    hiddenPackages: Set<String>,
    favoritePackages: Set<String>,
    onLaunch: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onCopyPackage: (AppInfo) -> Unit,
    onInspect: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onSetHidden: (Collection<String>, Boolean) -> Unit,
    onUninstall: (Collection<String>) -> Unit,
    onDevShortcut: (String) -> Boolean,
    onOpenSettings: () -> Unit,
    columns: Int,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var sheetApp by remember { mutableStateOf<AppInfo?>(null) }

    fun exitSelection() {
        selectionMode = false
        selected = emptySet()
    }

    fun toggle(pkg: String) {
        selected = if (pkg in selected) selected - pkg else selected + pkg
    }

    val filtered = remember(apps, query, showHidden, hiddenPackages) {
        apps.asSequence()
            .filter { showHidden || it.packageName !in hiddenPackages }
            .filter {
                val q = query.trim()
                q.isBlank() ||
                    it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
            .toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        if (selectionMode) {
            SelectionBar(
                count = selected.size,
                onCancel = ::exitSelection,
                onHide = { onSetHidden(selected, true); exitSelection() },
                onUninstall = { onUninstall(selected); exitSelection() },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Apps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Launcher settings")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text("Search apps or package") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showHidden = !showHidden }) {
                        Icon(
                            if (showHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (showHidden) "Hide hidden apps" else "Show hidden apps",
                            tint = if (showHidden) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DEV_SHORTCUTS.forEach { shortcut ->
                    AssistChip(
                        onClick = {
                            if (!onDevShortcut(shortcut.action)) {
                                Toast.makeText(context, "Not available on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        label = { Text(shortcut.label) },
                        leadingIcon = { Icon(shortcut.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (apps.isEmpty()) "Loading apps…" else "No apps match your search",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppGridItem(
                        app = app,
                        selected = app.packageName in selected,
                        hidden = app.packageName in hiddenPackages,
                        iconSize = iconSize,
                        onClick = { if (selectionMode) toggle(app.packageName) else onLaunch(app) },
                        onLongClick = { if (selectionMode) toggle(app.packageName) else sheetApp = app },
                    )
                }
            }
        }
    }

    sheetApp?.let { app ->
        AppActionSheet(
            app = app,
            isFavorite = app.packageName in favoritePackages,
            isHidden = app.packageName in hiddenPackages,
            onDismiss = { sheetApp = null },
            onOpenAppInfo = { onOpenAppInfo(app) },
            onCopyPackage = { onCopyPackage(app) },
            onInspect = { onInspect(app) },
            onToggleFavorite = { onToggleFavorite(app) },
            onToggleHidden = { onSetHidden(listOf(app.packageName), app.packageName !in hiddenPackages) },
            onUninstall = { onUninstall(listOf(app.packageName)) },
            onSelectMultiple = {
                selectionMode = true
                selected = setOf(app.packageName)
            },
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = "Cancel") }
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp).weight(1f),
        )
        IconButton(onClick = onHide, enabled = count > 0) {
            Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hide selected")
        }
        IconButton(onClick = onUninstall, enabled = count > 0) {
            Icon(Icons.Rounded.Delete, contentDescription = "Uninstall selected")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppInfo,
    selected: Boolean,
    hidden: Boolean,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp)
            .alpha(if (hidden) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon = app.icon, contentDescription = app.label, modifier = Modifier.size(iconSize))
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
