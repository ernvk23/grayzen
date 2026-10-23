package com.ernvk23.grayzen.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GrayzenBackground = Color(0xFF12100E)
val GrayzenSurface = Color(0xFF1B1815)
val GrayzenSurfaceRaised = Color(0xFF25211D)
val GrayzenText = Color(0xFFF5EFE7)
val GrayzenMuted = Color(0xFFB9B0A6)
val GrayzenBorder = Color(0xFF403832)
val GrayzenAmber = Color(0xFFE3A85B)
val GrayzenAmberSoft = Color(0xFFFFD8A8)
val GrayzenError = Color(0xFFFFB4AB)
val GrayzenDisabled = Color(0xFF746C64)

private val GrayzenColors: ColorScheme = darkColorScheme(
    primary = GrayzenAmber,
    onPrimary = GrayzenBackground,
    primaryContainer = Color(0xFF4B3521),
    onPrimaryContainer = GrayzenAmberSoft,
    secondary = GrayzenAmberSoft,
    onSecondary = GrayzenBackground,
    background = GrayzenBackground,
    onBackground = GrayzenText,
    surface = GrayzenSurface,
    onSurface = GrayzenText,
    surfaceVariant = GrayzenSurfaceRaised,
    onSurfaceVariant = GrayzenMuted,
    outline = GrayzenBorder,
    outlineVariant = GrayzenBorder,
    error = GrayzenError,
    onError = GrayzenBackground,
)

private val GrayzenTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 43.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.45).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp,
    ),
)

@Composable
fun GrayzenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrayzenColors,
        typography = GrayzenTypography,
        content = content,
    )
}
