package com.pawmino.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Pawmino theme — a single warm, teal-forward light scheme ("Soft Orbit Care Planner").
 * Dynamic color is intentionally disabled so the app keeps a consistent, non-medical
 * identity across devices. A calm light scheme is used regardless of system dark mode.
 */
private val PawminoColorScheme = lightColorScheme(
    primary = PawTeal,
    onPrimary = SurfaceWhite,
    primaryContainer = SoftMint,
    onPrimaryContainer = DeepTeal,
    secondary = DeepTeal,
    onSecondary = SurfaceWhite,
    secondaryContainer = SoftMint,
    onSecondaryContainer = DeepTeal,
    tertiary = PlayAmber,
    onTertiary = SurfaceWhite,
    background = AppBackground,
    onBackground = DeepInk,
    surface = SurfaceWhite,
    onSurface = DeepInk,
    surfaceVariant = WarmCream,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = SurfaceWhite
)

@Composable
fun PawminoTheme(content: @Composable () -> Unit) {
    // Intentionally ignore isSystemInDarkTheme() to preserve the branded light palette.
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = PawminoColorScheme,
        typography = PawminoTypography,
        content = content
    )
}
