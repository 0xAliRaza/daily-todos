package com.aliraza.dailytodos.ui.components

import com.aliraza.dailytodos.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class QuickDateTest {

    private val wednesday = LocalDate.of(2026, 11, 18)

    @Test
    fun theQuickDaysAreTodayTomorrowAndNextMonday() {
        assertEquals(
            listOf(
                QuickDate(R.string.day_today, wednesday),
                QuickDate(R.string.day_tomorrow, LocalDate.of(2026, 11, 19)),
                QuickDate(R.string.date_next_week, LocalDate.of(2026, 11, 23)),
            ),
            quickDates(wednesday, DayOfWeek.MONDAY),
        )
    }

    @Test
    fun nextWeekFollowsTheWeekStart() {
        assertEquals(LocalDate.of(2026, 11, 22), quickDates(wednesday, DayOfWeek.SUNDAY).last().date)
        assertEquals(LocalDate.of(2026, 11, 21), quickDates(wednesday, DayOfWeek.SATURDAY).last().date)
    }
}
