package com.studypilot.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CamelPrimary,
    onPrimary = WarmWhite,
    primaryContainer = CreamSurfaceVariant,
    onPrimaryContainer = DarkChocolate,
    secondary = LightBrown,
    onSecondary = WarmWhite,
    secondaryContainer = CreamSurface,
    onSecondaryContainer = DarkChocolate,
    tertiary = LightBrownAccent,
    background = IvoryBackground,
    onBackground = DarkChocolate,
    surface = WarmWhite,
    onSurface = DarkChocolate,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = DarkChocolateMuted,
    outline = CardBorder,
    outlineVariant = SubtleDivider
)

@Composable
fun StudyPilotTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = IvoryBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
