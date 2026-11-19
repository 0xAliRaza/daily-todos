package com.aliraza.dailytodos.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

class CalendarRangeTest {

    private val wednesday = LocalDate.of(2026, 11, 18)

    @Test
    fun dayAndThreeDaysStartAtTheAnchor() {
        assertEquals(listOf(wednesday), visibleDays(CalendarMode.DAY, wednesday, MONDAY))
        assertEquals((0L..2).map { wednesday.plusDays(it) }, visibleDays(CalendarMode.THREE_DAYS, wednesday, MONDAY))
    }

    @Test
    fun aWeekRunsMondayToSunday() {
        val days = visibleDays(CalendarMode.WEEK, wednesday, MONDAY)
        assertEquals(LocalDate.of(2026, 11, 16), days.first())
        assertEquals(LocalDate.of(2026, 11, 22), days.last())
    }

    @Test
    fun aMonthShowsWholeWeeksCoveringIt() {
        val days = visibleDays(CalendarMode.MONTH, wednesday, MONDAY)
        assertEquals(LocalDate.of(2026, 10, 26), days.first())
        assertEquals(LocalDate.of(2026, 12, 6), days.last())
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(0, days.size % 7)
    }

    @Test
    fun aMonthStartingOnMondayHasNoLeadingDays() {
        val june = LocalDate.of(2026, 6, 10)
        assertEquals(LocalDate.of(2026, 6, 1), visibleDays(CalendarMode.MONTH, june, MONDAY).first())
    }

    @Test
    fun aWeekStartingOnSundayRunsSundayToSaturday() {
        val days = visibleDays(CalendarMode.WEEK, wednesday, SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 15), days.first())
        assertEquals(LocalDate.of(2026, 11, 21), days.last())
    }

    @Test
    fun aMonthOfSundayWeeksStartsAndEndsOnThem() {
        // 1 November 2026 is a Sunday, so the month has no leading days; it ends on Saturday 5 December.
        val days = visibleDays(CalendarMode.MONTH, wednesday, SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), days.first())
        assertEquals(LocalDate.of(2026, 12, 5), days.last())
        assertEquals(DayOfWeek.SATURDAY, days.last().dayOfWeek)
        assertEquals(0, days.size % 7)
    }

    @Test
    fun stepsMoveByTheViewsSpan() {
        assertEquals(wednesday.plusDays(1), step(CalendarMode.DAY, wednesday, forward = true))
        assertEquals(wednesday.minusDays(3), step(CalendarMode.THREE_DAYS, wednesday, forward = false))
        assertEquals(wednesday.plusWeeks(1), step(CalendarMode.WEEK, wednesday, forward = true))
        assertEquals(LocalDate.of(2026, 12, 18), step(CalendarMode.MONTH, wednesday, forward = true))
    }
}
