package com.umain.launcher.data

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Reads the list of launchable apps and performs app-management + inspection actions.
 * The whole "backend" of the launcher — PackageManager already knows everything.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

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

    /** Applies [uri] as the wallpaper (WallpaperManager FLAG_SYSTEM/FLAG_LOCK bitmask). */
    suspend fun applyWallpaper(uri: Uri, which: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                WallpaperManager.getInstance(context).setStream(input, null, true, which)
            }
        }.isSuccess
    }

    /** Copies a package's base APK to the cache and shares it (dev/pentest extraction). */
    suspend fun shareApk(packageName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val safeLabel = appInfo.loadLabel(packageManager).toString().replace(Regex("[^A-Za-z0-9._-]"), "_")
            val dir = File(context.cacheDir, "apks").apply { mkdirs() }
            val out = File(dir, "$safeLabel-$packageName.apk")
            File(appInfo.sourceDir).copyTo(out, overwrite = true)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, "Share APK")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }.isSuccess
    }

    /** Inspects a package for the Activity Launcher / pentest view. */
    @Suppress("DEPRECATION", "QueryPermissionsNeeded")
    suspend fun inspectPackage(packageName: String): PackageDetails? = withContext(Dispatchers.IO) {
        val sigFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or
            PackageManager.GET_PERMISSIONS or sigFlag

        val info = runCatching { packageManager.getPackageInfo(packageName, flags) }.getOrNull()
            ?: return@withContext null
        val appInfo = info.applicationInfo ?: return@withContext null

        val flagsArr = info.requestedPermissionsFlags
        val perms = info.requestedPermissions?.mapIndexed { i, name ->
            val granted = flagsArr != null && i < flagsArr.size &&
                (flagsArr[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            PermissionEntry(name, granted, protectionLevelOf(name))
        }?.sortedBy { it.shortName.lowercase() }.orEmpty()

        val exported = buildList {
            info.services?.forEach { if (it.exported) add(ComponentEntry(ComponentType.SERVICE, it.name, it.permission)) }
            info.receivers?.forEach { if (it.exported) add(ComponentEntry(ComponentType.RECEIVER, it.name, it.permission)) }
            info.providers?.forEach {
                if (it.exported) add(ComponentEntry(ComponentType.PROVIDER, it.name, it.readPermission ?: it.writePermission))
            }
        }.sortedBy { it.shortName.lowercase() }

        PackageDetails(
            packageName = packageName,
            label = appInfo.loadLabel(packageManager).toString(),
            versionName = info.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion,
            debuggable = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            allowsBackup = appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
            usesCleartextTraffic = appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0,
            isSystem = appInfo.isSystemApp(),
            signatureSha256 = signatureSha256Of(info),
            installerPackage = installerOf(packageName),
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            uid = appInfo.uid,
            sharedUserId = info.sharedUserId,
            apkPath = appInfo.sourceDir,
            permissions = perms,
            activities = info.activities.orEmpty().map { activity ->
                ActivityEntry(activity.name, activity.exported, activity.enabled)
            }.sortedBy { it.shortName.lowercase() },
            exportedComponents = exported,
        )
    }

    @Suppress("DEPRECATION")
    private fun signatureSha256Of(info: PackageInfo): String? = runCatching {
        val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            info.signatures?.firstOrNull()?.toByteArray()
        } ?: return null
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(":") { "%02X".format(it) }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun installerOf(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun protectionLevelOf(permission: String): ProtectionLevel = runCatching {
        val pi = packageManager.getPermissionInfo(permission, 0)
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.protection
        } else {
            pi.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        }
        when (base) {
            PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.DANGEROUS
            PermissionInfo.PROTECTION_SIGNATURE -> ProtectionLevel.SIGNATURE
            PermissionInfo.PROTECTION_NORMAL -> ProtectionLevel.NORMAL
            PermissionInfo.PROTECTION_INTERNAL -> ProtectionLevel.INTERNAL
            else -> ProtectionLevel.UNKNOWN
        }
    }.getOrDefault(ProtectionLevel.UNKNOWN)

    private fun ApplicationInfo.isSystemApp(): Boolean =
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    /** Reads battery / storage / memory for the home status widget. Cheap and synchronous. */
    fun readSystemStats(): SystemStats {
        val battery = (context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val fs = StatFs(Environment.getDataDirectory().absolutePath)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return SystemStats(
            batteryPercent = battery,
            storageFreeBytes = fs.availableBytes,
            storageTotalBytes = fs.totalBytes,
            memoryUsedBytes = mem.totalMem - mem.availMem,
            memoryTotalBytes = mem.totalMem,
        )
    }
}
