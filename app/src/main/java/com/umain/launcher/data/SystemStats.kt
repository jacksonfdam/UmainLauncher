package com.umain.launcher.data

/** A snapshot of device status shown by the home status widget. */
data class SystemStats(
    val batteryPercent: Int,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
)
