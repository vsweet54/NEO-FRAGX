package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeoFragxColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBlack,
    primaryContainer = NeonBlueDark,
    onPrimaryContainer = NeonCyan,
    secondary = NeonBlue,
    onSecondary = CyberBlack,
    secondaryContainer = CyberCardHover,
    onSecondaryContainer = NeonBlue,
    tertiary = NeonPurple,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardStroke,
    outlineVariant = CyberCardStrokeGlow
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberBlack.toArgb()
                window.navigationBarColor = CyberBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NeoFragxColorScheme,
        typography = Typography,
        content = content
    )
}
