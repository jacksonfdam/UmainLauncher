package com.umain.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.umain.launcher.data.ColorTheme
import com.umain.launcher.data.ThemeMode

private fun accentFor(theme: ColorTheme, dark: Boolean): Color = when (theme) {
    ColorTheme.PURPLE -> if (dark) AccentPurpleDark else AccentPurple
    ColorTheme.GREEN -> if (dark) AccentGreenDark else AccentGreen
    ColorTheme.BLUE -> if (dark) AccentBlueDark else AccentBlue
    ColorTheme.AMBER -> if (dark) AccentAmberDark else AccentAmber
    ColorTheme.MONO -> if (dark) AccentMonoDark else AccentMono
}

@Composable
fun UmainLauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    colorTheme: ColorTheme = ColorTheme.PURPLE,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val accent = accentFor(colorTheme, darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = accent,
            background = SurfaceDark,
            surface = SurfaceDark,
            onBackground = OnSurfaceDark,
            onSurface = OnSurfaceDark,
        )
        else -> lightColorScheme(primary = accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
