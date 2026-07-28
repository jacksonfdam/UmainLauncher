package com.umain.launcher.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_THRESHOLD = 8f

/**
 * The wallpaper-facing home screen: a large clock and a hint to swipe up.
 * A vertical drag upward opens the app drawer.
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Negative dragAmount == finger moving up the screen.
                    if (dragAmount < -SWIPE_THRESHOLD) onOpenDrawer()
                }
            },
    ) {
        Clock(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 96.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Swipe up to see your apps",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    // Re-emits the current time every second so the clock stays accurate
    // without a busy loop.
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 16.sp,
        )
    }
}
