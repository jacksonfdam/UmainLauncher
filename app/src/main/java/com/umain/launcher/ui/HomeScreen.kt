package com.umain.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umain.launcher.data.AppInfo
import com.umain.launcher.data.SystemStats
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_THRESHOLD = 8f

/**
 * The wallpaper-facing home screen: a large clock, a favorites dock, and a hint to
 * swipe up. A vertical drag upward opens the app drawer.
 */
@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchFavorite: (AppInfo) -> Unit,
    onUnpinFavorite: (AppInfo) -> Unit,
    showStatusWidget: Boolean,
    widgetOffsetX: Float,
    widgetOffsetY: Float,
    statsProvider: () -> SystemStats,
    onWidgetMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -SWIPE_THRESHOLD) onOpenDrawer()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            },
    ) {
        if (showStatusWidget) {
            StatusWidget(
                statsProvider = statsProvider,
                offsetX = widgetOffsetX,
                offsetY = widgetOffsetY,
                onMove = onWidgetMove,
            )
        }
        Clock(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 96.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (favorites.isNotEmpty()) {
                FavoritesDock(
                    favorites = favorites,
                    onLaunch = onLaunchFavorite,
                    onUnpin = onUnpinFavorite,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Text("Swipe up to see your apps", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesDock(
    favorites: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onUnpin: (AppInfo) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        favorites.take(5).forEach { app ->
            AppIcon(
                icon = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { onUnpin(app) }),
            )
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = timeFormat.format(now), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
        Text(text = dateFormat.format(now).replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 16.sp)
    }
}
