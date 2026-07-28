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
    /** e.g. `com.foo.bar.ui.SecretActivity` -> `SecretActivity`. */
    val shortName: String get() = className.substringAfterLast('.')
}

/**
 * A snapshot of a package worth eyeballing during dev/pentest work: version,
 * SDK levels, the security-relevant manifest flags, requested permissions and the
 * full activity list.
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
    val isSystem: Boolean,
    val permissions: List<String>,
    val activities: List<ActivityEntry>,
)
