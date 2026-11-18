package com.aliraza.dailytasks.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens: the only place colours are defined.
 *
 * One red accent on plain neutrals (PLAN.md section 8). Priorities and due dates carry their own
 * signal colours, so a list can be read at a glance. `ThemeContrastTest` fails if any pairing drops
 * below WCAG AA.
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
    /** Due-date text by nearness. Overdue uses [accentText]; later dates use [onSurfaceVariant]. */
    val dateToday: Color,
    val dateTomorrow: Color,
    val dateThisWeek: Color,

    /** Behind a row swiped right to reschedule; [onAccent] icons sit on it. */
    val swipeReschedule: Color,
)

val LightTokens = ColorTokens(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3F3F3),
    divider = Color(0xFFEBEBEB),
    onBackground = Color(0xFF202020),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFF8C8C8C),
    accent = Color(0xFFD1453B),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFC3392F),
    accentContainer = Color(0xFFFCEDEB),
    onAccentContainer = Color(0xFF8A1F17),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    priority1 = Color(0xFFD1453B),
    priority2 = Color(0xFFC76E00),
    priority3 = Color(0xFF246FE0),
    dateToday = Color(0xFF058527),
    dateTomorrow = Color(0xFFA35C00),
    dateThisWeek = Color(0xFF692EC2),
    swipeReschedule = Color(0xFF692EC2),
)

val DarkTokens = ColorTokens(
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF262626),
    surfaceVariant = Color(0xFF323232),
    divider = Color(0xFF333333),
    onBackground = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFADADAD),
    outline = Color(0xFF858585),
    accent = Color(0xFFD1453B),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFFF7A6E),
    accentContainer = Color(0xFF4A2421),
    onAccentContainer = Color(0xFFFFDAD5),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    priority1 = Color(0xFFFF7066),
    priority2 = Color(0xFFFF9A14),
    priority3 = Color(0xFF5C9DFF),
    dateToday = Color(0xFF3CC264),
    dateTomorrow = Color(0xFFFF9A14),
    dateThisWeek = Color(0xFFB287FF),
    swipeReschedule = Color(0xFF7440C9),
)
