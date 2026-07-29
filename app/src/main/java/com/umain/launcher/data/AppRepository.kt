package com.umain.launcher.data

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the list of launchable apps and performs app-management actions (launch,
 * uninstall, app-info, activity inspection). This is the whole "backend" of the
 * launcher — the PackageManager already knows everything about installed apps.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /** Loads every launchable app, sorted alphabetically. Runs off the main thread. */
    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val mainLauncherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        packageManager.queryIntentActivities(mainLauncherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null

                AppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager),
                    isSystem = activityInfo.applicationInfo.isSystemApp(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    /** Opens the system "App info" screen for [packageName]. */
    fun openAppInfo(packageName: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Fires an explicit component. Returns false if it can't be started (e.g. not exported). */
    fun launchActivity(packageName: String, className: String): Boolean = try {
        val intent = Intent().apply {
            component = ComponentName(packageName, className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    /** Fires a system Settings action (dev shortcuts). Returns false if unavailable. */
    fun openSettings(action: String): Boolean = try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    /** Opens the system wallpaper picker (no permission required). */
    fun openWallpaperPicker() {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Set wallpaper")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Applies [uri] as the wallpaper. [which] is a [WallpaperManager] FLAG_SYSTEM /
     * FLAG_LOCK bitmask. Requires the SET_WALLPAPER permission (declared in the manifest).
     */
    suspend fun applyWallpaper(uri: Uri, which: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                WallpaperManager.getInstance(context).setStream(input, null, true, which)
            }
        }.isSuccess
    }

    /** Inspects a package for the Activity Launcher / pentest view. */
    @Suppress("DEPRECATION", "QueryPermissionsNeeded")
    suspend fun inspectPackage(packageName: String): PackageDetails? = withContext(Dispatchers.IO) {
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
        val info = runCatching { packageManager.getPackageInfo(packageName, flags) }.getOrNull()
            ?: return@withContext null
        val appInfo = info.applicationInfo ?: return@withContext null

        PackageDetails(
            packageName = packageName,
            label = appInfo.loadLabel(packageManager).toString(),
            versionName = info.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion,
            debuggable = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            allowsBackup = appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
            isSystem = appInfo.isSystemApp(),
            permissions = info.requestedPermissions?.toList().orEmpty().sorted(),
            activities = info.activities.orEmpty().map { activity ->
                ActivityEntry(
                    className = activity.name,
                    exported = activity.exported,
                    enabled = activity.enabled,
                )
            }.sortedBy { it.shortName.lowercase() },
        )
    }

    private fun ApplicationInfo.isSystemApp(): Boolean =
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
