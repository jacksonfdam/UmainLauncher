package com.umain.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umain.launcher.data.AppInfo

/**
 * Long-press action sheet for a single app: info, copy package, inspect activities,
 * pin, hide and uninstall — plus an entry point into multi-select mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppInfo,
    isFavorite: Boolean,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onCopyPackage: () -> Unit,
    onInspect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onUninstall: () -> Unit,
    onSelectMultiple: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(icon = app.icon, contentDescription = null, modifier = Modifier.size(44.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider()

            Action(Icons.Rounded.Info, "App info") { onOpenAppInfo(); onDismiss() }
            Action(Icons.Rounded.ContentCopy, "Copy package name") { onCopyPackage(); onDismiss() }
            Action(Icons.AutoMirrored.Rounded.Launch, "Activities (dev/pentest)") { onInspect(); onDismiss() }
            Action(
                if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (isFavorite) "Unpin from dock" else "Pin to dock",
            ) { onToggleFavorite(); onDismiss() }
            Action(
                if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                if (isHidden) "Unhide" else "Hide from drawer",
            ) { onToggleHidden(); onDismiss() }
            if (!app.isSystem) {
                Action(Icons.Rounded.Delete, "Uninstall") { onUninstall(); onDismiss() }
            }

            HorizontalDivider()

            Action(Icons.Rounded.Checklist, "Select multiple") { onSelectMultiple(); onDismiss() }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
