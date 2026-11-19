package com.aliraza.dailytodos.ui.components

import com.aliraza.dailytodos.core.model.ClockFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class TimeFormatTest {
    private val afternoon = LocalTime.of(15, 12)

    @Test
    fun twelveHourIsTheDefaultLook() {
        assertEquals("3:12 PM", formatTime(afternoon, ClockFormat.H12, Locale.US))
        assertEquals("12:05 AM", formatTime(LocalTime.of(0, 5), ClockFormat.H12, Locale.US))
    }

    @Test
    fun twentyFourHourPadsTheHour() {
        assertEquals("15:12", formatTime(afternoon, ClockFormat.H24, Locale.US))
        assertEquals("09:00", formatTime(LocalTime.of(9, 0), ClockFormat.H24, Locale.US))
    }

    @Test
    fun gridHoursAreShort() {
        assertEquals("2 PM", hourLabel(14, ClockFormat.H12, Locale.US))
        assertEquals("14", hourLabel(14, ClockFormat.H24, Locale.US))
    }
}
