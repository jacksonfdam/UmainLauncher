package com.umain.launcher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.umain.launcher.data.WidgetPlacement
import kotlin.math.roundToInt

private const val SNAP_STEP_DP = 16f
private const val MIN_SCALE = 0.6f
private const val MAX_SCALE = 2.0f

/**
 * Wraps a home element so it can be dragged (snapping to a [SNAP_STEP_DP] grid on
 * release) and, when [resizable], scaled via a corner handle. Position/scale are a
 * [WidgetPlacement] offset from the element's natural anchor; changes are committed
 * to persistence via [onCommit] on gesture end.
 *
 * The caller supplies the anchor via [modifier] (e.g. `Modifier.align(TopCenter)`).
 */
@Composable
fun MovableWidget(
    placement: WidgetPlacement,
    resizable: Boolean,
    onCommit: (WidgetPlacement) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var dx by remember { mutableFloatStateOf(placement.dx) }
    var dy by remember { mutableFloatStateOf(placement.dy) }
    var scale by remember { mutableFloatStateOf(placement.scale) }
    // Adopt persisted/late-loaded values (won't fire mid-gesture — we commit on end).
    LaunchedEffect(placement) {
        dx = placement.dx
        dy = placement.dy
        scale = placement.scale
    }

    Box(modifier = modifier.offset(dx.dp, dy.dp)) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            dx = (dx / SNAP_STEP_DP).roundToInt() * SNAP_STEP_DP
                            dy = (dy / SNAP_STEP_DP).roundToInt() * SNAP_STEP_DP
                            onCommit(WidgetPlacement(dx, dy, scale))
                        },
                    ) { change, drag ->
                        change.consume()
                        dx += drag.x.toDp().value
                        dy += drag.y.toDp().value
                    }
                },
        ) {
            content()
        }

        if (resizable) {
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onCommit(WidgetPlacement(dx, dy, scale)) },
                        ) { change, drag ->
                            change.consume()
                            scale = (scale + (drag.x + drag.y).toDp().value / 160f)
                                .coerceIn(MIN_SCALE, MAX_SCALE)
                        }
                    },
            ) {
                Icon(
                    Icons.Rounded.OpenInFull,
                    contentDescription = "Resize",
                    modifier = Modifier.size(12.dp).offset(5.dp, 5.dp),
                )
            }
        }
    }
}
