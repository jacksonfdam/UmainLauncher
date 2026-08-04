package com.umain.launcher.ui

import android.app.WallpaperManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umain.launcher.data.COLUMN_RANGE
import com.umain.launcher.data.ColorTheme
import com.umain.launcher.data.IconFilter
import com.umain.launcher.data.IconShape
import com.umain.launcher.data.IconSize
import com.umain.launcher.data.LauncherSettings
import com.umain.launcher.data.LayoutMode
import com.umain.launcher.data.ThemeMode

/**
 * Launcher configuration: grid density, icon color filter, theme, and wallpaper.
 * Reached by long-pressing the home screen or the gear in the app drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onLayout: (LayoutMode) -> Unit,
    onColumns: (Int) -> Unit,
    onIconSize: (IconSize) -> Unit,
    onIconFilter: (IconFilter) -> Unit,
    onIconShape: (IconShape) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onColorTheme: (ColorTheme) -> Unit,
    onShowStatusWidget: (Boolean) -> Unit,
    onResetLayout: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenSystemWallpaper: () -> Unit,
    onApplyWallpaper: (Uri, Int) -> Unit,
    onBack: () -> Unit,
) {
    var pendingWallpaper by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingWallpaper = uri
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // --- Layout ---
                Section("Layout") {
                    ChipRow(LayoutMode.entries, settings.layout, onLayout) { it.displayName() }
                    if (settings.layout == LayoutMode.MINIMAL) {
                        Text(
                            "Text-only app list — no icons, no distractions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // --- Grid & icons (only relevant in the Grid layout) ---
                if (settings.layout == LayoutMode.GRID) {
                    Section("Grid") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Columns", Modifier.weight(1f))
                            IconButton(
                                onClick = { onColumns(settings.columns - 1) },
                                enabled = settings.columns > COLUMN_RANGE.first,
                            ) { Icon(Icons.Rounded.Remove, contentDescription = "Fewer columns") }
                            Text(settings.columns.toString(), style = MaterialTheme.typography.titleMedium)
                            IconButton(
                                onClick = { onColumns(settings.columns + 1) },
                                enabled = settings.columns < COLUMN_RANGE.last,
                            ) { Icon(Icons.Rounded.Add, contentDescription = "More columns") }
                        }
                        Text("Icon size", style = MaterialTheme.typography.bodyMedium)
                        ChipRow(IconSize.entries, settings.iconSize, onIconSize) { it.displayName() }
                        Text("Icon shape", style = MaterialTheme.typography.bodyMedium)
                        ChipRow(IconShape.entries, settings.iconShape, onIconShape) { it.displayName() }
                    }

                    Section("Icon filter") {
                        ChipRow(IconFilter.entries, settings.iconFilter, onIconFilter) { it.displayName() }
                    }
                }

                // --- Theme ---
                Section("Theme") {
                    ChipRow(ThemeMode.entries, settings.themeMode, onThemeMode) { it.displayName() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dynamic color")
                            Text(
                                "Material You colors (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.dynamicColor, onCheckedChange = onDynamicColor)
                    }
                    if (!settings.dynamicColor) {
                        Text("Accent", style = MaterialTheme.typography.bodyMedium)
                        ChipRow(ColorTheme.entries, settings.colorTheme, onColorTheme) { it.displayName() }
                    }
                }

                // --- Home ---
                Section("Home") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Status widget")
                            Text(
                                "Draggable battery / storage / memory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.showStatusWidget, onCheckedChange = onShowStatusWidget)
                    }
                    Text(
                        "Drag widgets to move, drag the corner handle to resize; they snap to a grid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onAddWidget, modifier = Modifier.fillMaxWidth()) {
                        Text("Add widget")
                    }
                    OutlinedButton(onClick = onResetLayout, modifier = Modifier.fillMaxWidth()) {
                        Text("Reset home layout")
                    }
                }

                // --- Wallpaper ---
                Section("Wallpaper") {
                    Button(onClick = onOpenSystemWallpaper, modifier = Modifier.fillMaxWidth()) {
                        Text("System wallpaper picker")
                    }
                    OutlinedButton(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose from gallery") }
                }
            }
        }
    }

    pendingWallpaper?.let { uri ->
        WallpaperTargetDialog(
            onDismiss = { pendingWallpaper = null },
            onPick = { which ->
                onApplyWallpaper(uri, which)
                pendingWallpaper = null
            },
        )
    }
}

@Composable
private fun WallpaperTargetDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply wallpaper to") },
        text = {
            Column {
                TextButton(onClick = { onPick(WallpaperManager.FLAG_SYSTEM) }) { Text("Home screen") }
                TextButton(onClick = { onPick(WallpaperManager.FLAG_LOCK) }) { Text("Lock screen") }
                TextButton(
                    onClick = { onPick(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                ) { Text("Both") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

private fun Enum<*>.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
