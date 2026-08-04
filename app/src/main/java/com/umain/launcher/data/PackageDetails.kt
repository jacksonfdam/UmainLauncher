package com.umain.launcher.data

/**
 * A launchable/exported activity inside a package. Used by the Activity Launcher
 * (a dev/pentest tool: it lets you fire any exported activity directly).
 */
data class ActivityEntry(
    val className: String,
    val exported: Boolean,
    val enabled: Boolean,
) {
    val shortName: String get() = className.substringAfterLast('.')
}

/** Manifest component kinds we surface as attack surface. */
enum class ComponentType { SERVICE, RECEIVER, PROVIDER }

/** An exported service/receiver/provider — attack surface, especially with no permission. */
data class ComponentEntry(
    val type: ComponentType,
    val className: String,
    val permission: String?,
) {
    val shortName: String get() = className.substringAfterLast('.')
    val unprotected: Boolean get() = permission.isNullOrBlank()
}

/** Android permission protection level. */
enum class ProtectionLevel { NORMAL, DANGEROUS, SIGNATURE, INTERNAL, UNKNOWN }

/** A requested permission plus its runtime grant state and protection level. */
data class PermissionEntry(
    val name: String,
    val granted: Boolean,
    val protection: ProtectionLevel,
) {
    val shortName: String get() = name.substringAfterLast('.')
}

/**
 * A security-oriented snapshot of a package for dev/pentest work.
 */
data class PackageDetails(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val debuggable: Boolean,
    val allowsBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val isSystem: Boolean,
    val signatureSha256: String?,
    val installerPackage: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val uid: Int,
    val sharedUserId: String?,
    val apkPath: String?,
    val permissions: List<PermissionEntry>,
    val activities: List<ActivityEntry>,
    val exportedComponents: List<ComponentEntry>,
) {
    val dangerousGranted: List<PermissionEntry>
        get() = permissions.filter { it.protection == ProtectionLevel.DANGEROUS && it.granted }
}
