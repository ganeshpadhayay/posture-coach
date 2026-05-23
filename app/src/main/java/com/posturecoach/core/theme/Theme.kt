package com.posturecoach.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal700,
    onPrimary = Sand50,
    primaryContainer = Sand100,
    onPrimaryContainer = Teal900,
    secondary = Coral500,
    onSecondary = Sand50,
    background = Surface,
    onBackground = Charcoal900,
    surface = Surface,
    onSurface = Charcoal900,
    surfaceVariant = Sand100,
    onSurfaceVariant = Charcoal600,
    error = ErrorRed,
    onError = Sand50,
    outline = Charcoal300,
)

private val DarkColors = darkColorScheme(
    primary = Teal500,
    onPrimary = Charcoal900,
    primaryContainer = Teal900,
    onPrimaryContainer = Sand50,
    secondary = Coral500,
    onSecondary = Charcoal900,
    background = SurfaceDark,
    onBackground = Sand50,
    surface = SurfaceDark,
    onSurface = Sand50,
    surfaceVariant = Charcoal600,
    onSurfaceVariant = Charcoal300,
    error = ErrorRed,
    onError = Sand50,
    outline = Charcoal600,
)

@Composable
fun PostureCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PostureCoachTypography,
        content = content,
    )
}
