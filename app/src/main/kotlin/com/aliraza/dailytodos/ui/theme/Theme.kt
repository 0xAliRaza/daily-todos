package com.aliraza.dailytodos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aliraza.dailytodos.core.model.Accent

/** Maps the design tokens onto Material 3. No wallpaper-based dynamic colour. */
fun ColorTokens.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        inversePrimary = inverseAction,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        tertiary = rest,
        onTertiary = onRest,
        tertiaryContainer = restContainer,
        onTertiaryContainer = onRestContainer,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = fill,
        onSurfaceVariant = ink2,
        surfaceTint = Color.Transparent,
        surfaceBright = sheet,
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = sheet,
        surfaceContainer = sheet,
        surfaceContainerHigh = sheet,
        surfaceContainerHighest = fill,
        inverseSurface = inverse,
        inverseOnSurface = onInverse,
        outline = ink3,
        outlineVariant = hairline,
        error = danger,
        onError = onDanger,
        scrim = Color.Black,
    )
}

private val LocalColorTokens = staticCompositionLocalOf { LightTokens }

/** The full token set, for the signal colours Material's scheme has no slot for. */
val MaterialTheme.tokens: ColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalColorTokens.current

/**
 * The type scale from the design's Tokens board, Roboto only. Each role sits in one Material slot so
 * Material's own components pick it up; the named getters below are what call sites use.
 */
private fun typography(): Typography {
    val base = Typography()
    fun style(size: Int, weight: Int, line: Int, tracking: Float = 0f) = TextStyle(
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )
    return base.copy(
        displayLarge = style(72, 300, 80, -1f),
        displayMedium = style(44, 800, 50, -0.8f),
        displaySmall = style(32, 800, 38, -0.6f),
        headlineMedium = style(32, 800, 38, -0.6f),
        headlineSmall = style(24, 700, 30, -0.3f),
        titleLarge = style(20, 700, 26, -0.2f),
        titleMedium = style(18, 800, 24, -0.2f),
        titleSmall = style(15, 700, 20),
        bodyLarge = style(16, 400, 22),
        bodyMedium = style(15, 400, 20),
        bodySmall = style(13, 500, 18),
        labelLarge = style(14, 600, 20, 0.1f),
        labelMedium = style(12, 700, 16, 0.6f),
        labelSmall = style(12, 600, 16, 0.1f),
    )
}

private val AppTypography = typography()

/** The focus timer's countdown: 72 sp light. */
val Typography.timer: TextStyle get() = displayLarge

/** The focus stats' all-time headline: 44 sp extra-bold. */
val Typography.statHeadline: TextStyle get() = displayMedium

/** A tab's large title: 32 sp extra-bold. */
val Typography.screenTitle: TextStyle get() = headlineMedium

/** A sheet's or the focus timer's task title: 24 sp bold. */
val Typography.sheetTitle: TextStyle get() = headlineSmall

/** The title in a collapsed header: 20 sp bold. */
val Typography.barTitle: TextStyle get() = titleLarge

/** A section header such as "Overdue": 18 sp extra-bold. */
val Typography.section: TextStyle get() = titleMedium

/** A day in Upcoming such as "Mon 23 Nov": 15 sp bold. */
val Typography.dayHeader: TextStyle get() = titleSmall

/** A task's title in a list: 16 sp. */
val Typography.taskTitle: TextStyle get() = bodyLarge

/** A settings row or body text: 15 sp. */
val Typography.body: TextStyle get() = bodyMedium

/** Buttons and chips: 14 sp semi-bold. */
val Typography.button: TextStyle get() = labelLarge

/** A row's meta line: 13 sp medium, in ink-2. */
val Typography.meta: TextStyle get() = bodySmall

/** A settings group label, written in capitals: 12 sp bold, tracked. */
val Typography.groupLabel: TextStyle get() = labelMedium

/** A tab's label: 12 sp semi-bold. */
val Typography.tab: TextStyle get() = labelSmall

@Composable
fun DailyTodosTheme(
    dark: Boolean = isSystemInDarkTheme(),
    black: Boolean = false,
    accent: Accent = Accent.INK_BLUE,
    content: @Composable () -> Unit,
) {
    val tokens = tokensFor(dark, black, accent)
    CompositionLocalProvider(LocalColorTokens provides tokens) {
        MaterialTheme(
            colorScheme = tokens.toColorScheme(dark),
            typography = AppTypography,
            content = content,
        )
    }
}
