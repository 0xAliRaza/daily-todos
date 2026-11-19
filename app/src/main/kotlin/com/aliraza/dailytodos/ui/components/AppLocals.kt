package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.text.util.LocalePreferences
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.Settings
import java.time.DayOfWeek

/** How times are written across the app. */
val LocalClockFormat = staticCompositionLocalOf { ClockFormat.H12 }

/** The day weeks start on, already resolved from the setting and the phone. */
val LocalWeekStart = staticCompositionLocalOf { DayOfWeek.MONDAY }

/** Whether taps that change something give a short tick. */
val LocalHapticsOn = staticCompositionLocalOf { true }

/** Provides the display settings every screen reads: the clock, the week start and haptics. */
@Composable
fun ProvideDisplaySettings(settings: Settings, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalClockFormat provides settings.clockFormat,
        LocalWeekStart provides settings.weekStart.resolve(phoneFirstDay()),
        LocalHapticsOn provides settings.haptics,
        content = content,
    )
}

/** The first day of the week from the phone's regional settings (Android 14+), else its locale. */
fun phoneFirstDay(): DayOfWeek = when (LocalePreferences.getFirstDayOfWeek()) {
    LocalePreferences.FirstDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
    LocalePreferences.FirstDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
    LocalePreferences.FirstDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
    LocalePreferences.FirstDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
    LocalePreferences.FirstDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
    LocalePreferences.FirstDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
    else -> DayOfWeek.MONDAY
}
