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

private val AppColorScheme = lightColorScheme(
    primary = WandePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBF3FF),
    onPrimaryContainer = WandePrimaryDark,
    secondary = WandeSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = WandeCyan,
    background = WandeBackgroundLight,
    onBackground = WandeTextPrimaryLight,
    surface = WandeSurfaceLight,
    onSurface = WandeTextPrimaryLight,
    surfaceVariant = WandeSurfaceVariantLight,
    onSurfaceVariant = WandeTextSecondaryLight,
    outline = WandeBorder,
    error = StatusError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force pure white / bright theme as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = AppColorScheme, typography = Typography, content = content)
}


