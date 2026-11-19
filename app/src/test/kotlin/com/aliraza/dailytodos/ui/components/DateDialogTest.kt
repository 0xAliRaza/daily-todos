package com.aliraza.dailytodos.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/** The picker reads its first weekday from WeekFields, so the locale it gets must carry the week start. */
class DateDialogTest {

    @Test
    fun theLocaleCarriesTheWeekStart() {
        DayOfWeek.entries.forEach { day ->
            assertEquals(day, WeekFields.of(Locale.US.withFirstDay(day)).firstDayOfWeek)
            assertEquals(day, WeekFields.of(Locale.GERMANY.withFirstDay(day)).firstDayOfWeek)
        }
    }

    @Test
    fun onlyTheWeekStartChanges() {
        val tagged = Locale.UK.withFirstDay(DayOfWeek.SUNDAY)
        assertEquals("en-GB-u-fw-sun", tagged.toLanguageTag())
        assertEquals(Locale.UK.language, tagged.language)
        assertEquals(Locale.UK.country, tagged.country)
    }
}
