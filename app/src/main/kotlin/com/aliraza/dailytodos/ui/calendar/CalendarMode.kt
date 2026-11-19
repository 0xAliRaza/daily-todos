package com.aliraza.dailytodos.ui.calendar

import com.aliraza.dailytodos.core.time.DAYS_IN_WEEK
import com.aliraza.dailytodos.core.time.endOfWeek
import com.aliraza.dailytodos.core.time.startOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The calendar's views. [span] is how many days one step moves; a month moves by a month. */
enum class CalendarMode(val span: Long) { DAY(1), THREE_DAYS(3), WEEK(7), MONTH(0) }

/**
 * The days a view shows around [anchor], for weeks that start on [firstDay]. A month shows whole weeks,
 * from the start of the week holding the 1st to the end of the week holding the last day, so it has four
 * to six rows.
 */
fun visibleDays(mode: CalendarMode, anchor: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
    val (first, count) = when (mode) {
        CalendarMode.DAY, CalendarMode.THREE_DAYS -> anchor to mode.span

        CalendarMode.WEEK -> anchor.startOfWeek(firstDay) to DAYS_IN_WEEK

        CalendarMode.MONTH -> {
            val start = anchor.withDayOfMonth(1).startOfWeek(firstDay)
            val end = anchor.with(TemporalAdjusters.lastDayOfMonth()).endOfWeek(firstDay)
            start to ChronoUnit.DAYS.between(start, end) + 1
        }
    }
    return (0 until count).map { first.plusDays(it) }
}

/** The anchor one step back or forward. */
fun step(mode: CalendarMode, anchor: LocalDate, forward: Boolean): LocalDate {
    val sign = if (forward) 1L else -1L
    return if (mode == CalendarMode.MONTH) anchor.plusMonths(sign) else anchor.plusDays(sign * mode.span)
}
