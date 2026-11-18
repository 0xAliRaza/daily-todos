package com.aliraza.dailytasks.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens: the only place colours are defined.
 *
 * One ink-blue accent on plain neutrals (PLAN.md section 8). Red is kept for what is urgent: overdue
 * dates, P1 and deleting. Priorities carry their own signal colours; due dates are red when overdue,
 * the accent for today and grey after that, so a list stays calm. `ThemeContrastTest` fails if any
 * pairing drops below WCAG AA.
 */
data class ColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    /** Hairlines between rows. Decorative, so it has no contrast requirement. */
    val divider: Color,
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
    /** Check-box rings and flags, P1 to P3. Graphics, so 3:1. */
    val priority1: Color,
    val priority2: Color,
    val priority3: Color,
    /** Overdue due dates. Today uses [accentText]; later dates use [onSurfaceVariant]. */
    val overdue: Color,
    /** The focus timer's ring during a break. A graphic, so 3:1. */
    val rest: Color,
)

val LightTokens = ColorTokens(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3F3F3),
    divider = Color(0xFFEBEBEB),
    onBackground = Color(0xFF202020),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFF8C8C8C),
    accent = Color(0xFF2563EB),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFF2563EB),
    accentContainer = Color(0xFFEAF1FE),
    onAccentContainer = Color(0xFF1E3A8A),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    priority1 = Color(0xFFD1453B),
    priority2 = Color(0xFFC76E00),
    priority3 = Color(0xFF246FE0),
    overdue = Color(0xFFC3392F),
    rest = Color(0xFF058527),
)

val DarkTokens = ColorTokens(
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF262626),
    surfaceVariant = Color(0xFF323232),
    divider = Color(0xFF333333),
    onBackground = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFADADAD),
    outline = Color(0xFF858585),
    accent = Color(0xFF7AA2FF),
    onAccent = Color(0xFF0B1A3A),
    accentText = Color(0xFF8DB0FF),
    accentContainer = Color(0xFF1F2A44),
    onAccentContainer = Color(0xFFDCE7FF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    priority1 = Color(0xFFFF7066),
    priority2 = Color(0xFFFF9A14),
    priority3 = Color(0xFF5C9DFF),
    overdue = Color(0xFFFF7A6E),
    rest = Color(0xFF3CC264),
)
