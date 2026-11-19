package com.aliraza.dailytodos.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The calendar's views. [span] is how many days one step moves; a month moves by a month. */
enum class CalendarMode(val span: Long) { DAY(1), THREE_DAYS(3), WEEK(7), MONTH(0) }

private const val WEEK = 7L

private fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * The days a view shows around [anchor]. A week runs Monday to Sunday. A month shows whole weeks, from
 * the Monday on or before the 1st to the Sunday on or after the last day, so it has four to six rows.
 */
fun visibleDays(mode: CalendarMode, anchor: LocalDate): List<LocalDate> {
    val (first, count) = when (mode) {
        CalendarMode.DAY, CalendarMode.THREE_DAYS -> anchor to mode.span

        CalendarMode.WEEK -> anchor.weekStart() to WEEK

        CalendarMode.MONTH -> {
            val start = anchor.withDayOfMonth(1).weekStart()
            val end = anchor.with(
                TemporalAdjusters.lastDayOfMonth(),
            ).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
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
