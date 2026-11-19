package com.aliraza.dailytodos.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The first day of the week holding this date, for weeks that start on [first]. */
fun LocalDate.startOfWeek(first: DayOfWeek): LocalDate = with(TemporalAdjusters.previousOrSame(first))

/** The last day of the week holding this date, for weeks that start on [first]. */
fun LocalDate.endOfWeek(first: DayOfWeek): LocalDate = startOfWeek(first).plusDays(DAYS_IN_WEEK - 1)

/** The start of the week after this date's week. */
fun LocalDate.nextWeekStart(first: DayOfWeek): LocalDate = startOfWeek(first).plusDays(DAYS_IN_WEEK)

const val DAYS_IN_WEEK = 7L
