package com.aliraza.dailytasks.ui.theme

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Maps the design tokens onto Material 3. No wallpaper-based dynamic colour. */
fun ColorTokens.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        secondary = accentText,
        onSecondary = background,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
        outline = outline,
        outlineVariant = divider,
        error = error,
        onError = onError,
    )
}

private val LocalColorTokens = staticCompositionLocalOf { LightTokens }

/** The full token set, for the signal colours Material's scheme has no slot for. */
val MaterialTheme.tokens: ColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalColorTokens.current

/** Material's type scale without its wide letter spacing, which reads loose in dense lists. */
private fun typography(): Typography {
    val base = Typography()
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.sp),
    )
}

@Composable
fun DailyTasksTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (dark) DarkTokens else LightTokens
    // The snackbar sits on the opposite theme, so its action takes the other theme's accent text.
    val inverse = if (dark) LightTokens else DarkTokens
    CompositionLocalProvider(LocalColorTokens provides tokens) {
        MaterialTheme(
            colorScheme = tokens.toColorScheme(dark).copy(inversePrimary = inverse.accentText),
            typography = typography(),
            content = content,
        )
    }
}
