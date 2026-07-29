package com.umain.launcher.data

/**
 * Position/size of a movable home widget, stored as a dp offset from the widget's
 * natural anchor plus a scale factor. Device-portable (dp, relative to anchor).
 */
data class WidgetPlacement(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
)

/** Stable ids for the home widgets. */
object WidgetIds {
    const val CLOCK = "clock"
    const val DOCK = "dock"
    const val STATUS = "status"
}
