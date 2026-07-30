package com.umain.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.umain.launcher.data.WidgetPlacement
import kotlin.math.roundToInt

private const val SNAP_STEP_DP = 16f
private const val MIN_SCALE = 0.6f
private const val MAX_SCALE = 2.0f

/**
 * Wraps a home element so it can be dragged (snapping to a grid on release) and,
 * when [resizable], scaled via a corner handle. Position/scale are a [WidgetPlacement]
 * offset from the element's natural anchor (supplied via [modifier], e.g.
 * `Modifier.align(TopCenter)`); changes commit via [onCommit] on gesture end.
 *
 * @param dragViaHandle when true the body is NOT draggable (so interactive content
 *   like a hosted AppWidget keeps its own touches); a move handle provides dragging.
 * @param onRemove when non-null, shows a remove (×) handle.
 */
@Composable
fun MovableWidget(
    placement: WidgetPlacement,
    resizable: Boolean,
    onCommit: (WidgetPlacement) -> Unit,
    modifier: Modifier = Modifier,
    dragViaHandle: Boolean = false,
    onRemove: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var dx by remember { mutableFloatStateOf(placement.dx) }
    var dy by remember { mutableFloatStateOf(placement.dy) }
    var scale by remember { mutableFloatStateOf(placement.scale) }
    LaunchedEffect(placement) {
        dx = placement.dx
        dy = placement.dy
        scale = placement.scale
    }

    fun commitSnapped() {
        dx = (dx / SNAP_STEP_DP).roundToInt() * SNAP_STEP_DP
        dy = (dy / SNAP_STEP_DP).roundToInt() * SNAP_STEP_DP
        onCommit(WidgetPlacement(dx, dy, scale))
    }

    Box(modifier = modifier.offset(dx.dp, dy.dp)) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (!dragViaHandle) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(onDragEnd = { commitSnapped() }) { change, drag ->
                                change.consume()
                                dx += drag.x.toDp().value
                                dy += drag.y.toDp().value
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }

        if (dragViaHandle) {
            HandleSurface(
                icon = Icons.Rounded.OpenWith,
                description = "Move",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { commitSnapped() }) { change, drag ->
                            change.consume()
                            dx += drag.x.toDp().value
                            dy += drag.y.toDp().value
                        }
                    },
            )
        }

        if (onRemove != null) {
            HandleSurface(
                icon = Icons.Rounded.Close,
                description = "Remove",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onRemove),
            )
        }

        if (resizable) {
            HandleSurface(
                icon = Icons.Rounded.OpenInFull,
                description = "Resize",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { onCommit(WidgetPlacement(dx, dy, scale)) }) { change, drag ->
                            change.consume()
                            scale = (scale + (drag.x + drag.y).toDp().value / 160f).coerceIn(MIN_SCALE, MAX_SCALE)
                        }
                    },
            )
        }
    }
}

@Composable
private fun HandleSurface(icon: ImageVector, description: String, modifier: Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(14.dp))
        }
    }
}
