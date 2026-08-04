package com.umain.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Thin wrapper around [AppWidgetHost] + [AppWidgetManager] so the launcher can host
 * third-party home-screen widgets. Created in the Activity (its lifecycle drives
 * start/stopListening) and provided to Compose via [LocalAppWidgetHost].
 */
class AppWidgetHostController(private val context: Context) {

    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context.applicationContext, HOST_ID)

    fun startListening() = host.startListening()
    fun stopListening() = host.stopListening()

    fun allocateId(): Int = host.allocateAppWidgetId()
    fun deleteId(id: Int) = host.deleteAppWidgetId(id)

    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)

    @Suppress("DEPRECATION")
    fun createView(id: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(id) ?: return null
        return host.createView(context, id, info)
    }

    companion object {
        const val HOST_ID = 0x55AA
    }
}

/** The launcher's widget host, provided by the Activity. Null when unavailable. */
val LocalAppWidgetHost = staticCompositionLocalOf<AppWidgetHostController?> { null }

/**
 * Renders a hosted AppWidget by id. Returns an empty box if the id is no longer
 * bound (e.g. the provider was uninstalled).
 */
@Composable
fun HostedAppWidget(appWidgetId: Int, modifier: Modifier = Modifier) {
    val host = LocalAppWidgetHost.current ?: return
    val context = LocalContext.current
    val info = remember(appWidgetId) { host.info(appWidgetId) } ?: return

    AndroidView(
        modifier = modifier,
        factory = { host.createView(appWidgetId) ?: View(context) },
        update = { view ->
            if (view is AppWidgetHostView) {
                val min = info.minWidth.coerceAtLeast(80)
                view.updateAppWidgetSize(Bundle(), min, info.minHeight.coerceAtLeast(80), min * 3, info.minHeight * 3)
            }
        },
    )
}
