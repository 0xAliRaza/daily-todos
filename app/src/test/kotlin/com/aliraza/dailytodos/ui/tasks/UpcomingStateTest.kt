package com.aliraza.dailytodos.ui.tasks

import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class UpcomingStateTest {
    /** Saturday 21 November 2026. */
    private val today = LocalDate.of(2026, 11, 21)
    private fun day(offset: Long) = today.plusDays(offset)

    private fun task(id: String, date: LocalDate, priority: Priority = Priority.NONE) = Task(
        id = id,
        listId = "daily",
        title = id,
        priority = priority,
        dueDate = date,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    /** The board's week: tomorrow two tasks, Monday one, Friday one, nothing Tue–Thu or Sat, then Later. */
    private val byDay = mapOf(
        day(1) to listOf(task("mum", day(1)), task("tap", day(1))),
        day(2) to listOf(task("expenses", day(2))),
        day(6) to listOf(task("flights", day(6))),
        day(11) to listOf(task("dentist", day(11))),
        day(40) to listOf(task("far", day(40))),
    )
    private val plan = UpcomingPlan.of(today, byDay)

    private fun describe(line: UpcomingLine): String = when (line) {
        is UpcomingLine.Week -> "week ${line.count}"
        is UpcomingLine.Day -> "day ${line.date.dayOfMonth}"
        is UpcomingLine.Free -> "free ${line.first.dayOfMonth}-${line.last.dayOfMonth}"
        is UpcomingLine.Later -> "later ${line.count}"
        is UpcomingLine.Item -> if (line.later) "later:${line.task.id}" else line.task.id
    }

    @Test
    fun theWeekListsDaysWithTasksFoldsFreeRunsAndEndsWithLater() {
        assertEquals(
            listOf(
                "week 4",
                "day 22", "mum", "tap",
                "day 23", "expenses",
                "free 24-26",
                "day 27", "flights",
                "free 28-28",
                "later 2", "later:dentist", "later:far",
            ),
            plan.lines.map(::describe),
        )
    }

    @Test
    fun aWeekWithNothingPlannedIsOneFreeLineAndNoLaterWithoutLaterTasks() {
        assertEquals(listOf("week 0", "free 22-28"), UpcomingPlan.of(today, emptyMap()).lines.map(::describe))
    }

    @Test
    fun theEighthDayAlreadyBelongsToLater() {
        val lines = UpcomingPlan.of(today, mapOf(day(8) to listOf(task("x", day(8))))).lines
        assertEquals(listOf("week 0", "free 22-28", "later 1", "later:x"), lines.map(::describe))
    }

    @Test
    fun aStripDayJumpsToItsHeaderOrTheFreeRunHoldingIt() {
        assertEquals(1, plan.indexOf(day(1)))
        assertEquals(4, plan.indexOf(day(2)))
        assertEquals(6, plan.indexOf(day(3)))
        assertEquals(6, plan.indexOf(day(5)))
        assertEquals(7, plan.indexOf(day(6)))
        assertEquals(9, plan.indexOf(day(7)))
    }

    @Test
    fun aStripDayBeyondTheWeekJumpsIntoLaterAtTheFirstTaskOnOrAfterIt() {
        assertEquals(11, plan.indexOf(day(8)))
        assertEquals(11, plan.indexOf(day(11)))
        assertEquals(12, plan.indexOf(day(12)))
        // Past every task, the jump goes as far as the list does.
        assertEquals(12, plan.indexOf(day(60)))
        // Today and earlier have no line of their own: the top.
        assertEquals(0, plan.indexOf(today))
    }

    @Test
    fun theStripFollowsTheDayOfTheTopLine() {
        assertEquals(day(1), plan.dayAt(0))
        assertEquals(day(1), plan.dayAt(2))
        assertEquals(day(2), plan.dayAt(4))
        assertEquals(day(3), plan.dayAt(6))
        assertEquals(day(8), plan.dayAt(10))
        assertEquals(day(40), plan.dayAt(12))
        assertEquals(day(40), plan.dayAt(99))
    }

    @Test
    fun stripDotsPutTheHighestPriorityFirstAndStopAtThree() {
        val tasks = listOf(Priority.NONE, Priority.P3, Priority.P1, Priority.NONE, Priority.P2)
            .mapIndexed { i, p -> task("t$i", day(1), p) }
        assertEquals(listOf(Priority.P1, Priority.P2, Priority.P3), stripDots(tasks))
        assertEquals(listOf(Priority.P3, Priority.NONE), stripDots(tasks.take(2)))
        assertEquals(emptyList<Priority>(), stripDots(emptyList()))
    }

    @Test
    fun theStripCannotPageBeforeThisWeekAndTodayComesHome() {
        val today = LocalDate.of(2026, 11, 18)
        val state = UpcomingState(today, DayOfWeek.MONDAY)
        assertEquals(null, state.page(forward = false))
        assertEquals(state.thisWeek, state.weekStart)
        assertEquals(LocalDate.of(2026, 11, 23), state.page(forward = true))
        assertEquals(LocalDate.of(2026, 11, 19), state.page(forward = false))
        state.selected = today.plusDays(9)
        assertEquals(false, state.isHome)
        state.home()
        assertEquals(true, state.isHome)
    }

    @Test
    fun aSwipeShowsTheWeekItSettledOnOnce() {
        val today = LocalDate.of(2026, 11, 18)
        val state = UpcomingState(today, DayOfWeek.MONDAY)
        // This week is already shown, and there is nothing before it or past the last page.
        assertEquals(null, state.showWeek(0))
        assertEquals(null, state.showWeek(-1))
        assertEquals(null, state.showWeek(UpcomingState.WEEKS))
        assertEquals(LocalDate.of(2026, 11, 30), state.showWeek(2))
        assertEquals(2, state.week)
        assertEquals(null, state.showWeek(2))
        // The list following a day into another week moves the page with it.
        state.follow(LocalDate.of(2026, 12, 9))
        assertEquals(3, state.week)
    }

    @Test
    fun followingADayBringsItsWeekIntoTheStrip() {
        val state = UpcomingState(LocalDate.of(2026, 11, 18), DayOfWeek.MONDAY)
        state.follow(LocalDate.of(2026, 12, 3))
        assertEquals(LocalDate.of(2026, 11, 30), state.weekStart)
        assertEquals(LocalDate.of(2026, 12, 3), state.selected)
    }

    @Test
    fun aSundayWeekRunsTheStripFromSunday() {
        val state = UpcomingState(LocalDate.of(2026, 11, 18), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 15), state.thisWeek)
        assertEquals(LocalDate.of(2026, 11, 22), state.page(forward = true))
        // Saturday still belongs to the week that started the Sunday before.
        state.follow(LocalDate.of(2026, 12, 5))
        assertEquals(LocalDate.of(2026, 11, 29), state.weekStart)
    }
}
