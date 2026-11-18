package com.aliraza.dailytasks.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens: the only place colours are defined.
 *
 * Approved palette (PLAN.md section 8). `ThemeContrastTest` fails if any pairing drops below WCAG AA.
 *
 * Bright orange ([ColorTokens.accent]) is for fills and icons only; text in the accent colour uses
 * [ColorTokens.accentText], the darker shade.
 */
data class ColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val error: Color,
    val onError: Color,
)

val LightTokens = ColorTokens(
    background = Color(0xFFFCF8F4),
    surface = Color(0xFFFFFBF8),
    surfaceVariant = Color(0xFFF3EBE3),
    onBackground = Color(0xFF221B16),
    onSurfaceVariant = Color(0xFF5C5047),
    outline = Color(0xFF8A7B70),
    accent = Color(0xFFE26D00),
    onAccent = Color(0xFF1F1300),
    accentText = Color(0xFFA34900),
    accentContainer = Color(0xFFFFE3CC),
    onAccentContainer = Color(0xFF3A1C00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

val DarkTokens = ColorTokens(
    background = Color(0xFF1B1714),
    surface = Color(0xFF231E1A),
    surfaceVariant = Color(0xFF2F2823),
    onBackground = Color(0xFFF2EAE3),
    onSurfaceVariant = Color(0xFFCABDB2),
    outline = Color(0xFF928478),
    accent = Color(0xFFFF9A3D),
    onAccent = Color(0xFF2A1500),
    accentText = Color(0xFFFFB574),
    accentContainer = Color(0xFF5A2D00),
    onAccentContainer = Color(0xFFFFDCC2),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)
