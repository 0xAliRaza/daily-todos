package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val WEEK_DAYS = 7L

/** "Today", "Tomorrow", "Yesterday", a weekday within the coming week, else "Mon, 23 Nov". */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> stringResource(R.string.day_today)

        days == 1L -> stringResource(R.string.day_tomorrow)

        days == -1L -> stringResource(R.string.day_yesterday)

        days in 2 until WEEK_DAYS -> date.dayOfWeek.getDisplayName(
            TextStyle.FULL,
            LocalConfiguration.current.locales[0],
        )

        else -> date.format(SHORT_DATE)
    }
}

fun timeLabel(time: LocalTime): String = time.format(TIME)

/**
 * A day's section title: "Tomorrow · Thu 19 Nov". Yesterday, today and tomorrow lead with their name;
 * other days are just "Fri 20 Nov".
 */
@Composable
fun sectionDay(day: LocalDate, today: LocalDate): String {
    val near = abs(ChronoUnit.DAYS.between(today, day)) <= 1
    return listOfNotNull(dayLabel(day, today).takeIf { near }, sectionDate(day, today)).joinToString(" · ")
}

/** "Fri 20 Nov", with the year added when it isn't this year's. */
fun sectionDate(day: LocalDate, today: LocalDate): String =
    day.format(if (day.year == today.year) SECTION_DAY else SECTION_DAY_YEAR)

/** How near a due date is. It picks the date's colour, so a list can be read at a glance. */
enum class DateTone { OVERDUE, TODAY, TOMORROW, THIS_WEEK, LATER }

/** The tone of a task's due date as of [now]; null when it has no date. An open task past its time is overdue. */
fun Task.dueTone(now: LocalDateTime): DateTone? = dueDate?.let { date ->
    val today = now.toLocalDate()
    val passed = date == today && dueTime?.isBefore(now.toLocalTime()) == true
    dateTone(date, today, late = isOpen && (passed || date.isBefore(today)))
}

/** [late] marks a task due today whose time has passed: it reads as overdue too. */
fun dateTone(date: LocalDate, today: LocalDate, late: Boolean = false): DateTone {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        late || days < 0 -> DateTone.OVERDUE
        days == 0L -> DateTone.TODAY
        days == 1L -> DateTone.TOMORROW
        days < WEEK_DAYS -> DateTone.THIS_WEEK
        else -> DateTone.LATER
    }
}

fun longDateLabel(date: LocalDate): String = date.format(LONG_DATE)

@Composable
fun weekdayLong(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])

@Composable
fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0])

private val TIME: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val SECTION_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val SECTION_DAY_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
private val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
