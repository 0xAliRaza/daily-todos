package com.aliraza.dailytodos.core.focus

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FocusStatsTest {
    private val zone = ZoneId.of("Europe/Zurich")

    /** Thursday. */
    private val today = LocalDate.of(2026, 11, 19)

    private fun session(task: String?, day: LocalDate, minutes: Long, at: LocalTime = LocalTime.NOON): FocusSession {
        val ended = day.atTime(at).atZone(zone).toInstant()
        return FocusSession(task, ended.minus(Duration.ofMinutes(minutes)), ended, Duration.ofMinutes(minutes))
    }

    private val sessions = listOf(
        session("a", today, 25),
        session("a", today, 25),
        session("b", today.minusDays(1), 50),
        // Sunday: in the seven days, in this week only when weeks start on Sunday.
        session("b", today.minusDays(4), 15),
        session("gone", today.minusDays(2), 10),
        session(null, today.minusDays(20), 30),
    )
    private val titles = mapOf("a" to "Write report", "b" to "Plan trip")

    private fun stats() = FocusStats.from(sessions, titles, today, zone)

    @Test
    fun totalsForAllTimeTodayAndThisWeek() {
        val stats = stats()
        assertEquals(Duration.ofMinutes(155), stats.total)
        assertEquals(Duration.ofMinutes(50), stats.today)
        assertEquals(Duration.ofMinutes(110), stats.thisWeek(DayOfWeek.MONDAY))
        assertEquals(6, stats.sessions)
    }

    @Test
    fun aWeekStartingOnSundayCountsLastSunday() {
        assertEquals(Duration.ofMinutes(125), stats().thisWeek(DayOfWeek.SUNDAY))
    }

    @Test
    fun aWeekStartingTodayCountsOnlyToday() {
        assertEquals(Duration.ofMinutes(50), stats().thisWeek(DayOfWeek.THURSDAY))
    }

    @Test
    fun sevenDaysEndingToday() {
        val days = stats().lastDays
        assertEquals((6L downTo 0).map { today.minusDays(it) }, days.map { it.day })
        assertEquals(listOf(0L, 0L, 15L, 0L, 10L, 50L, 50L), days.map { it.focused.toMinutes() })
    }

    @Test
    fun aSessionCountsOnTheLocalDayItEnded() {
        val lateNight = session("a", today.minusDays(1), 10, LocalTime.of(23, 55))
        val stats = FocusStats.from(listOf(lateNight), titles, today, zone)
        assertEquals(Duration.ZERO, stats.today)
        assertEquals(Duration.ofMinutes(10), stats.lastDays[5].focused)
    }

    @Test
    fun topTasksGroupDeletedOnesTogether() {
        assertEquals(
            listOf(
                TaskFocus("b", "Plan trip", Duration.ofMinutes(65)),
                TaskFocus("a", "Write report", Duration.ofMinutes(50)),
                TaskFocus(null, null, Duration.ofMinutes(40)),
            ),
            stats().topTasks,
        )
    }

    @Test
    fun atMostFiveTopTasks() {
        val many = (1..8).map { session("t$it", today, it.toLong()) }
        val stats = FocusStats.from(many, (1..8).associate { "t$it" to "Task $it" }, today, zone)
        assertEquals((8 downTo 4).map { "t$it" }, stats.topTasks.map { it.taskId })
    }

    @Test
    fun nothingRecordedYet() {
        val stats = FocusStats.from(emptyList(), emptyMap(), today, zone)
        assertEquals(Duration.ZERO, stats.total)
        assertEquals(7, stats.lastDays.size)
        assertEquals(emptyList<TaskFocus>(), stats.topTasks)
    }
}
