package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DoneDayTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val day = LocalDate.of(2026, 11, 19)

    private fun on(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun session(taskId: String?, ended: Instant, minutes: Long) =
        FocusSession(taskId, ended.minus(Duration.ofMinutes(minutes)), ended, Duration.ofMinutes(minutes))

    private val report = task("report", completedAt = on(day, 15, 12))
    private val call = task("call", completedAt = on(day, 9, 30))
    private val yesterday = task("old", completedAt = on(day.minusDays(1), 23, 59))
    private val open = task("open")

    @Test
    fun theDaysDoneTasksInTheOrderTheyWereTicked() {
        val done = doneOn(day, zone, listOf(report, yesterday, call, open), emptyList())
        assertEquals(listOf("call", "report"), done.tasks.map { it.task.id })
        assertEquals(listOf(LocalTime.of(9, 30), LocalTime.of(15, 12)), done.tasks.map { it.at })
        assertEquals(Duration.ZERO, done.focused)
    }

    @Test
    fun eachCarriesItsFocusFromSessionsEndedThatDay() {
        val sessions = listOf(
            session("report", on(day, 11), 25),
            session("report", on(day, 14), 20),
            // Ended the day before, so it counts there.
            session("report", on(day.minusDays(1), 22), 50),
            session("call", on(day, 9), 15),
            // A task not done that day stays out of the total.
            session("open", on(day, 10), 40),
            session(null, on(day, 8), 10),
        )
        val done = doneOn(day, zone, listOf(report, call), sessions)
        assertEquals(listOf(Duration.ofMinutes(15), Duration.ofMinutes(45)), done.tasks.map { it.focused })
        assertEquals(Duration.ofMinutes(60), done.focused)
    }

    @Test
    fun theDayFollowsTheZone() {
        val lateEvening = task("late", completedAt = on(day, 23, 30))
        val inTokyo = doneOn(day.plusDays(1), ZoneId.of("Asia/Tokyo"), listOf(lateEvening), emptyList())
        assertEquals(listOf("late"), inTokyo.tasks.map { it.task.id })
        assertEquals(DoneDay.None, doneOn(day.plusDays(1), zone, listOf(lateEvening), emptyList()))
    }

    @Test
    fun byDayKeysEveryDayWithSomethingDone() {
        val byDay = doneByDay(zone, listOf(report, yesterday, call, open), emptyList())
        assertEquals(setOf(day, day.minusDays(1)), byDay.keys)
        assertEquals(listOf("call", "report"), byDay.getValue(day).tasks.map { it.task.id })
    }
}
