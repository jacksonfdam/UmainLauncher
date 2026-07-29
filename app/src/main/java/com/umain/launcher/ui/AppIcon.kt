package com.umain.launcher.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.umain.launcher.data.IconFilter
import com.umain.launcher.data.IconShape
import com.umain.launcher.data.IconSize

private const val ICON_PX = 144

/** The active icon color filter, provided once near the root (see LauncherApp). */
val LocalIconFilter = staticCompositionLocalOf { IconFilter.NONE }

/** The active icon mask shape, provided once near the root (see LauncherApp). */
val LocalIconShape = staticCompositionLocalOf { IconShape.SYSTEM }

/**
 * Renders a system [Drawable] app icon using `core-ktx`'s `toBitmap()` (no
 * third-party image loader), applying the user's [IconFilter] and [IconShape].
 */
@Composable
fun AppIcon(
    icon: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(icon) { icon.toBitmap(width = ICON_PX, height = ICON_PX).asImageBitmap() }
    val shape = LocalIconShape.current.toShape()
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = if (shape != null) modifier.clip(shape) else modifier,
        colorFilter = LocalIconFilter.current.toColorFilter(),
    )
}

/** dp size for each [IconSize] bucket. */
val IconSize.dp: Dp
    get() = when (this) {
        IconSize.SMALL -> 44.dp
        IconSize.MEDIUM -> 52.dp
        IconSize.LARGE -> 64.dp
    }

/** Mask shape for each [IconShape] (null == keep the icon's own shape). */
fun IconShape.toShape(): Shape? = when (this) {
    IconShape.SYSTEM -> null
    IconShape.CIRCLE -> CircleShape
    IconShape.SQUIRCLE -> RoundedCornerShape(percent = 45)
    IconShape.ROUNDED -> RoundedCornerShape(percent = 28)
}

/** Maps an [IconFilter] to a Compose [ColorFilter] (null == no filter). */
fun IconFilter.toColorFilter(): ColorFilter? = when (this) {
    IconFilter.NONE -> null
    IconFilter.GRAYSCALE -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    IconFilter.DESATURATED -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })
    IconFilter.SEPIA -> ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
