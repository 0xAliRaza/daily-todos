package com.aliraza.dailytasks.core.model

import java.time.Instant
import java.time.LocalTime

/** How many minutes before its time a timed task reminds. One setting for every task. */
enum class HeadsUp(val minutes: Long) {
    NONE(0),
    TEN(10),
    THIRTY(30),
    SIXTY(60),
    ;

    companion object {
        fun fromMinutes(minutes: Long): HeadsUp = entries.firstOrNull { it.minutes == minutes } ?: NONE
    }
}

/** What the focus timer does when a round ends. */
enum class TimerSignal { SOUND, VIBRATE, BOTH }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Every user setting. Stored as one row; the defaults here are the app's defaults. */
data class Settings(
    val defaultReminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val undatedGetsTomorrow: Boolean = true,
    val dateOnlyGetsReminder: Boolean = true,
    val headsUp: HeadsUp = HeadsUp.NONE,
    val overdueReminders: Boolean = true,
    val overdueReminderTime: LocalTime = DEFAULT_OVERDUE_TIME,
    val dndOverride: Boolean = false,
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    val focusCustomMinutes: Int = DEFAULT_FOCUS_CUSTOM_MINUTES,
    val breakCustomMinutes: Int = DEFAULT_BREAK_CUSTOM_MINUTES,
    val timerSignal: TimerSignal = TimerSignal.BOTH,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val lastBackupAt: Instant? = null,
) {
    companion object {
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(10, 0)
        val DEFAULT_OVERDUE_TIME: LocalTime = LocalTime.of(9, 0)
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val DEFAULT_FOCUS_CUSTOM_MINUTES = 90
        const val DEFAULT_BREAK_CUSTOM_MINUTES = 20
        val FOCUS_MINUTES_RANGE = 1..180
        val BREAK_MINUTES_RANGE = 1..60
    }
}
