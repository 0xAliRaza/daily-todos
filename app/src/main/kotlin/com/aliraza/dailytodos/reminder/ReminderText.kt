package com.aliraza.dailytodos.reminder

import com.aliraza.dailytodos.ui.components.formatTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The body line of a reminder notification: when the task is due and which list holds it.
 *
 * "Today 4:00 PM · Daily", "In 10 min · 11:30 AM · Work" ahead of time, "Overdue · Tue 9:00 AM · Daily"
 * once the day has passed. A task without a time leaves the time out ("Today · Daily").
 */
object ReminderText {
    private const val SEPARATOR = " · "
    private const val WEEK_DAYS = 7L
    private const val MINUTES_PER_HOUR = 60L
    private const val SECONDS_PER_MINUTE = 60L

    /** The words the body is made of, from string resources in the app and plain English in tests. */
    data class Words(
        val today: String,
        val tomorrow: String,
        val overdue: String,
        val inMinutes: (Int) -> String,
        val inHours: (Int) -> String,
    )

    /** [text], with the range of the word that is drawn bold and red ("Overdue"), if any. */
    data class Body(val text: String, val emphasis: IntRange? = null)

    /** The body of [content] posted at [now], the phone's local time. */
    fun body(content: ReminderContent, now: LocalDateTime, words: Words, locale: Locale = Locale.getDefault()): Body {
        val time = content.dueTime?.let { formatTime(it, content.clockFormat, locale) }
        val lead = when (content.kind) {
            ReminderKind.OVERDUE -> words.overdue
            ReminderKind.HEADS_UP -> countdown(content, now, words)
            ReminderKind.DUE -> joinWithSpace(dayWord(content.dueDate, now.toLocalDate(), words, locale), time)
        }
        val moment = when (content.kind) {
            ReminderKind.OVERDUE -> joinWithSpace(pastDay(content.dueDate, now.toLocalDate(), locale), time)
            ReminderKind.HEADS_UP -> time
            ReminderKind.DUE -> null
        }
        val text = listOfNotNull(lead, moment, content.listName?.takeIf { it.isNotBlank() }).joinToString(SEPARATOR)
        val emphasis = if (content.kind == ReminderKind.OVERDUE) words.overdue.indices else null
        return Body(text, emphasis)
    }

    /** "In 10 min", or "In 1 h" on the hour; rounded up, so a reminder never claims less time than left. */
    private fun countdown(content: ReminderContent, now: LocalDateTime, words: Words): String {
        val due = content.dueDate.atTime(content.dueTime ?: return words.inMinutes(0))
        val seconds = Duration.between(now, due).seconds.coerceAtLeast(0)
        val minutes = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
        return if (minutes > 0 && minutes % MINUTES_PER_HOUR == 0L) {
            words.inHours((minutes / MINUTES_PER_HOUR).toInt())
        } else {
            words.inMinutes(minutes.toInt())
        }
    }

    /** "Today", "Tomorrow", a short weekday within the week ahead, else "25 Nov". */
    private fun dayWord(date: LocalDate, today: LocalDate, words: Words, locale: Locale): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> words.today
            days == 1L -> words.tomorrow
            days in 2 until WEEK_DAYS -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            else -> date.format(DateTimeFormatter.ofPattern(DAY_MONTH, locale))
        }
    }

    /** A short weekday within the past week ("Tue"), else "18 Nov". */
    private fun pastDay(date: LocalDate, today: LocalDate, locale: Locale): String =
        if (ChronoUnit.DAYS.between(date, today) in 1 until WEEK_DAYS) {
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        } else {
            date.format(DateTimeFormatter.ofPattern(DAY_MONTH, locale))
        }

    private fun joinWithSpace(first: String, second: String?) = listOfNotNull(first, second).joinToString(" ")

    private const val DAY_MONTH = "d MMM"
}

/** Which reminder a notification is: on time, ahead of time (the heads-up), or on a later morning. */
enum class ReminderKind {
    DUE,
    HEADS_UP,
    OVERDUE,
    ;

    companion object {
        /**
         * [OVERDUE] once the due day is before [today]; [HEADS_UP] while a timed task's moment is still
         * ahead of [now]; otherwise [DUE].
         */
        fun of(dueDate: LocalDate, dueTime: LocalTime?, now: LocalDateTime): ReminderKind = when {
            dueDate.isBefore(now.toLocalDate()) -> OVERDUE
            dueTime != null && dueDate.atTime(dueTime).isAfter(now) -> HEADS_UP
            else -> DUE
        }
    }
}
