package com.example.bloodsugar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenContainer,
    onPrimary = OnPrimaryGreenContainer,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = OnPrimaryGreen,
    secondary = SecondaryBlueContainer,
    onSecondary = OnSecondaryBlueContainer,
    secondaryContainer = SecondaryBlue,
    onSecondaryContainer = OnSecondaryBlue,
    tertiary = TertiaryOrangeContainer,
    onTertiary = OnTertiaryOrangeContainer,
    tertiaryContainer = TertiaryOrange,
    onTertiaryContainer = OnTertiaryOrange,
    error = ErrorRedContainer,
    onError = OnErrorRedContainer,
    errorContainer = ErrorRed,
    onErrorContainer = OnErrorRed,
    background = OnBackgroundDark,
    onBackground = BackgroundLight,
    surface = OnSurfaceDark,
    onSurface = SurfaceLight,
    surfaceVariant = OnSurfaceVariantDark,
    onSurfaceVariant = SurfaceVariantLight,
    outline = OutlineGray
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    primaryContainer = PrimaryGreenContainer,
    onPrimaryContainer = OnPrimaryGreenContainer,
    secondary = SecondaryBlue,
    onSecondary = OnSecondaryBlue,
    secondaryContainer = SecondaryBlueContainer,
    onSecondaryContainer = OnSecondaryBlueContainer,
    tertiary = TertiaryOrange,
    onTertiary = OnTertiaryOrange,
    tertiaryContainer = TertiaryOrangeContainer,
    onTertiaryContainer = OnTertiaryOrangeContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundDark,
    surface = SurfaceLight,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineGray
)

@Composable
fun BloodSugarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val configuration = LocalConfiguration.current
    val dimensions = when {
        configuration.screenWidthDp <= 360 -> smallDimensions
        configuration.screenWidthDp <= 480 -> mediumDimensions
        else -> largeDimensions
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    CompositionLocalProvider(LocalDimensions provides dimensions) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}