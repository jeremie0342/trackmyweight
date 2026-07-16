package com.kps.trackmyweight.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = InkBackground,
    primaryContainer = AccentTealMuted,
    onPrimaryContainer = InkBackground,
    background = InkBackground,
    onBackground = InkTextPrimary,
    surface = InkSurface1,
    onSurface = InkTextPrimary,
    surfaceVariant = InkSurface2,
    onSurfaceVariant = InkTextSecondary,
    surfaceContainer = InkSurface2,
    surfaceContainerHigh = InkSurface3,
    surfaceContainerHighest = InkSurface3,
    outline = InkTextTertiary,
    outlineVariant = InkSurface3,
    error = SemanticNegative,
    onError = InkBackground,
)

private val LightScheme = lightColorScheme(
    primary = AccentTealMuted,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
)

@Composable
fun TrackMyWeightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = scheme.background.luminance() > 0.5f
            insets.isAppearanceLightNavigationBars = scheme.background.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}
