package com.umain.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the list of launchable apps from the system and starts them.
 *
 * This is the entire "backend" of a launcher: the PackageManager already knows
 * every installed app, so we just ask it for the ones that declare a
 * MAIN/LAUNCHER entry point.
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
                val packageName = resolveInfo.activityInfo.packageName
                    ?: return@mapNotNull null

                // Don't list ourselves in the drawer.
                if (packageName == context.packageName) return@mapNotNull null

                AppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Launches an app by package name. Silently ignores apps that can't be started. */
    fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
