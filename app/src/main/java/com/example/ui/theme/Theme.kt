package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WandePrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = WandePrimary,
    onPrimaryContainer = Color.White,
    secondary = WandeAccent,
    onSecondary = Color.Black,
    secondaryContainer = WandeAccentDark,
    onSecondaryContainer = Color.White,
    tertiary = WandeCyan,
    background = WandeBackgroundDark,
    onBackground = WandeTextPrimaryDark,
    surface = WandeSurfaceDark,
    onSurface = WandeTextPrimaryDark,
    surfaceVariant = WandeSurfaceVariantDark,
    onSurfaceVariant = WandeTextSecondaryDark,
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = WandePrimary,
    onPrimary = Color.White,
    primaryContainer = WandePrimaryLight.copy(alpha = 0.2f),
    onPrimaryContainer = WandePrimaryDark,
    secondary = WandeAccentDark,
    onSecondary = Color.White,
    secondaryContainer = WandeAccent.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = WandeCyan,
    background = WandeBackgroundLight,
    onBackground = WandeTextPrimaryLight,
    surface = WandeSurfaceLight,
    onSurface = WandeTextPrimaryLight,
    surfaceVariant = WandeSurfaceVariantLight,
    onSurfaceVariant = WandeTextSecondaryLight,
    error = StatusError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

