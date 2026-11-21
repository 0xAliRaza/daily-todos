package com.aliraza.dailytodos.reminder

import com.aliraza.dailytodos.core.model.ClockFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** The reminder body, against "now" Wednesday 2026-11-18 at 11:20. */
class ReminderTextTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val now = today.atTime(11, 20)
    private val words = ReminderText.Words(
        today = "Today",
        tomorrow = "Tomorrow",
        overdue = "Overdue",
        inMinutes = { "In $it min" },
        inHours = { "In $it h" },
    )

    private fun content(
        kind: ReminderKind,
        dueDate: LocalDate = today,
        dueTime: LocalTime? = LocalTime.of(16, 0),
        listName: String? = "Daily",
        clockFormat: ClockFormat = ClockFormat.H12,
    ) = ReminderContent("a", "Call bank", Instant.EPOCH, kind, dueDate, dueTime, listName, clockFormat)

    private fun body(content: ReminderContent, at: LocalDateTime = now) =
        ReminderText.body(content, at, words, Locale.US)

    @Test
    fun dueTodaySaysTodayTheTimeAndTheList() {
        val body = body(content(ReminderKind.DUE))
        assertEquals("Today 4:00 PM · Daily", body.text)
        assertNull(body.emphasis)
    }

    @Test
    fun aHeadsUpCountsDownToTheTime() {
        val body = body(content(ReminderKind.HEADS_UP, dueTime = LocalTime.of(11, 30), listName = "Work"))
        assertEquals("In 10 min · 11:30 AM · Work", body.text)
    }

    @Test
    fun aHeadsUpRoundsUpAndSaysAnHourAsHours() {
        val half = content(ReminderKind.HEADS_UP, dueTime = LocalTime.of(11, 30))
        assertEquals("In 10 min · 11:30 AM · Daily", body(half, now.plusSeconds(30)).text)
        val hour = content(ReminderKind.HEADS_UP, dueTime = LocalTime.of(12, 20))
        assertEquals("In 1 h · 12:20 PM · Daily", body(hour).text)
    }

    @Test
    fun overdueLeadsWithTheWordAndNamesTheDueDay() {
        val body = body(content(ReminderKind.OVERDUE, dueDate = today.minusDays(1), dueTime = LocalTime.of(9, 0)))
        assertEquals("Overdue · Tue 9:00 AM · Daily", body.text)
        assertEquals(0 until "Overdue".length, body.emphasis)
    }

    @Test
    fun overdueByMoreThanAWeekNamesTheDate() {
        val body = body(content(ReminderKind.OVERDUE, dueDate = LocalDate.of(2026, 11, 2), dueTime = null))
        assertEquals("Overdue · 2 Nov · Daily", body.text)
    }

    @Test
    fun aTaskWithoutATimeLeavesTheTimeOut() {
        assertEquals("Today · Daily", body(content(ReminderKind.DUE, dueTime = null)).text)
    }

    @Test
    fun aTaskOnALaterDayNamesTheDay() {
        assertEquals("Tomorrow 4:00 PM · Daily", body(content(ReminderKind.DUE, dueDate = today.plusDays(1))).text)
        assertEquals("Fri 4:00 PM · Daily", body(content(ReminderKind.DUE, dueDate = today.plusDays(2))).text)
    }

    @Test
    fun the24HourClockAndAMissingListAreFollowed() {
        val body = body(content(ReminderKind.DUE, listName = null, clockFormat = ClockFormat.H24))
        assertEquals("Today 16:00", body.text)
    }

    @Test
    fun theKindFollowsTheDueMoment() {
        val at = LocalTime.of(11, 30)
        assertEquals(ReminderKind.HEADS_UP, ReminderKind.of(today, at, now))
        assertEquals(ReminderKind.DUE, ReminderKind.of(today, at, today.atTime(11, 30)))
        assertEquals(ReminderKind.DUE, ReminderKind.of(today, null, now))
        assertEquals(ReminderKind.OVERDUE, ReminderKind.of(today.minusDays(1), at, now))
    }
}
