package com.umain.launcher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umain.launcher.data.SystemStats
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Draggable home widget showing battery, free storage and memory. The position is
 * committed to persistence on drag end; stats refresh every few seconds.
 */
@Composable
fun StatusWidget(
    statsProvider: () -> SystemStats,
    offsetX: Float,
    offsetY: Float,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ox by remember { mutableFloatStateOf(offsetX) }
    var oy by remember { mutableFloatStateOf(offsetY) }
    // Adopt persisted/late-loaded position (won't fire mid-drag, we commit on end).
    LaunchedEffect(offsetX, offsetY) { ox = offsetX; oy = offsetY }

    val stats by produceState(initialValue = statsProvider()) {
        while (true) {
            value = statsProvider()
            delay(4_000L)
        }
    }

    Surface(
        color = Color.Black.copy(alpha = 0.32f),
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { onMove(ox, oy) }) { change, drag ->
                    change.consume()
                    ox += drag.x
                    oy += drag.y
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatRow(
                Icons.Rounded.BatteryFull,
                if (stats.batteryPercent in 0..100) "${stats.batteryPercent}%" else "—",
            )
            StatRow(Icons.Rounded.Storage, "${gb(stats.storageFreeBytes)} free / ${gb(stats.storageTotalBytes)}")
            StatRow(Icons.Rounded.Memory, "${gb(stats.memoryUsedBytes)} / ${gb(stats.memoryTotalBytes)}")
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

private fun gb(bytes: Long): String =
    String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
