package com.pawmino.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 system typography (no bundled font files). A couple of styles are tuned for
 * the large loop percentage and section headers.
 */
private val default = Typography()

val PawminoTypography = Typography(
    displayLarge = default.displayLarge.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = default.labelMedium,
    bodyLarge = default.bodyLarge,
    bodyMedium = default.bodyMedium
)

/** Extra-large numeric style used for the loop completion percentage. */
val LoopPercentTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp
)
