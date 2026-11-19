package com.aliraza.dailytodos.core.time

import com.aliraza.dailytodos.core.model.WeekStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeeksTest {
    private val thursday = LocalDate.of(2026, 11, 19)

    @Test
    fun weeksStartOnTheChosenDay() {
        assertEquals(LocalDate.of(2026, 11, 16), thursday.startOfWeek(DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 15), thursday.startOfWeek(DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 21), thursday.endOfWeek(DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 22), thursday.endOfWeek(DayOfWeek.MONDAY))
    }

    @Test
    fun theStartDayItselfBeginsItsOwnWeek() {
        val sunday = LocalDate.of(2026, 11, 22)
        assertEquals(sunday, sunday.startOfWeek(DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 29), sunday.nextWeekStart(DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 23), sunday.nextWeekStart(DayOfWeek.MONDAY))
    }

    @Test
    fun thePhoneSettingFollowsThePhoneAndTheOthersAreFixed() {
        assertEquals(DayOfWeek.SATURDAY, WeekStart.PHONE.resolve(DayOfWeek.SATURDAY))
        assertEquals(DayOfWeek.MONDAY, WeekStart.MONDAY.resolve(DayOfWeek.SUNDAY))
        assertEquals(DayOfWeek.SUNDAY, WeekStart.SUNDAY.resolve(DayOfWeek.MONDAY))
    }
}
