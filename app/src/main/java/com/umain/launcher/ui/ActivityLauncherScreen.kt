package com.umain.launcher.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umain.launcher.data.AppInfo
import com.umain.launcher.data.ComponentEntry
import com.umain.launcher.data.PackageDetails
import com.umain.launcher.data.PermissionEntry
import com.umain.launcher.data.ProtectionLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dev/pentest inspector: package metadata (signature, installer, flags), permissions
 * with grant/protection, exported components, and the activity launcher. Also exports
 * the APK.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLauncherScreen(
    app: AppInfo,
    loadDetails: suspend (String) -> PackageDetails?,
    onLaunchActivity: (packageName: String, className: String) -> Boolean,
    onShareApk: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val details by produceState<PackageDetails?>(initialValue = null, app.packageName) {
        value = loadDetails(app.packageName)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onShareApk) {
                            Icon(Icons.Rounded.Share, contentDescription = "Export APK")
                        }
                    },
                )
            },
        ) { padding ->
            val data = details
            if (data == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                item { PackageHeader(data) }
                item { HorizontalDivider() }

                item { SectionTitle("Permissions (${data.permissions.size}) · ${data.dangerousGranted.size} dangerous granted") }
                items(data.permissions, key = { "p:${it.name}" }) { PermissionRow(it) }

                if (data.exportedComponents.isNotEmpty()) {
                    item { HorizontalDivider() }
                    item { SectionTitle("Exported components (${data.exportedComponents.size})") }
                    items(data.exportedComponents, key = { "c:${it.className}" }) { ComponentRow(it) }
                }

                item { HorizontalDivider() }
                item { SectionTitle("Activities (${data.activities.size})") }
                items(data.activities, key = { "a:${it.className}" }) { activity ->
                    ListItem(
                        headlineContent = { Text(activity.shortName) },
                        supportingContent = {
                            Text(
                                activity.className,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = { Badge(if (activity.exported) "exported" else "internal") },
                        modifier = Modifier.clickable {
                            if (!onLaunchActivity(data.packageName, activity.className)) {
                                Toast.makeText(
                                    context,
                                    "Couldn't launch (not exported or permission denied)",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageHeader(data: PackageDetails) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Field("Package", data.packageName)
        Field("Version", "${data.versionName ?: "?"} (${data.versionCode})")
        Field("SDK", "min ${data.minSdk} → target ${data.targetSdk}")
        Field("UID", data.uid.toString() + (data.sharedUserId?.let { " · shared: $it" } ?: ""))
        Field("Installer", data.installerPackage ?: "sideloaded / unknown")
        Field("Installed", dateFormat.format(Date(data.firstInstallTime)))
        Field("Updated", dateFormat.format(Date(data.lastUpdateTime)))
        data.signatureSha256?.let { Field("SHA-256", it) }
        data.apkPath?.let { Field("APK", it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (data.debuggable) Badge("debuggable", MaterialTheme.colorScheme.error)
            if (data.usesCleartextTraffic) Badge("cleartext", MaterialTheme.colorScheme.error)
            if (data.allowsBackup) Badge("allowBackup")
            if (data.isSystem) Badge("system")
        }
    }
}

@Composable
private fun PermissionRow(perm: PermissionEntry) {
    val protectionColor = when (perm.protection) {
        ProtectionLevel.DANGEROUS -> MaterialTheme.colorScheme.error
        ProtectionLevel.SIGNATURE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    ListItem(
        headlineContent = { Text(perm.shortName) },
        supportingContent = {
            Text(
                perm.name,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(perm.protection.name.lowercase(), protectionColor)
                if (perm.granted) Badge("granted", MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun ComponentRow(comp: ComponentEntry) {
    ListItem(
        headlineContent = { Text(comp.shortName) },
        supportingContent = {
            Text(
                comp.className,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(comp.type.name.lowercase())
                if (comp.unprotected) Badge("no-perm", MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Badge(text: String, container: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColorFor(container),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
