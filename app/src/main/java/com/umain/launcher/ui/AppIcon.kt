package com.umain.launcher.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

private const val ICON_PX = 144

/**
 * Renders a system [Drawable] app icon. Uses `core-ktx`'s `toBitmap()` (no
 * third-party image loader) and caches the raster per drawable instance.
 */
@Composable
fun AppIcon(
    icon: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(icon) { icon.toBitmap(width = ICON_PX, height = ICON_PX).asImageBitmap() }
    Image(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier)
}
