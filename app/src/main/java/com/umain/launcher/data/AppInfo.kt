package com.umain.launcher.data

import android.graphics.drawable.Drawable

/**
 * A single launchable application, as shown in the app drawer.
 *
 * @property label      user-visible name (already resolved from the PackageManager)
 * @property packageName unique package id used to launch the app
 * @property icon       the app's launcher icon
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)
