package com.aliraza.dailytodos.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class UpcomingStateTest {
    private val mon = LocalDate.of(2026, 11, 16)
    private val days = (0L..3L).map { mon.plusDays(it) }
    private val counts = mapOf(mon to 2, mon.plusDays(2) to 1)
    private val count = { day: LocalDate -> counts[day] ?: 0 }

    @Test
    fun aDaysHeaderSitsAfterEveryEarlierHeaderAndTask() {
        assertEquals(0, headerIndex(days, count, mon))
        assertEquals(3, headerIndex(days, count, mon.plusDays(1)))
        assertEquals(4, headerIndex(days, count, mon.plusDays(2)))
        assertEquals(6, headerIndex(days, count, mon.plusDays(3)))
    }

    @Test
    fun aDayTheListDoesNotShowHasNoHeader() {
        assertEquals(-1, headerIndex(days, count, mon.plusDays(9)))
    }

    @Test
    fun theListRunsAWeekAheadThenOnlyDaysWithTasks() {
        val today = LocalDate.of(2026, 11, 18)
        val state = UpcomingState(today)
        val far = LocalDate.of(2027, 1, 4)
        val days = state.days(setOf(far, today.plusDays(3)))
        assertEquals(today.plusDays(1), days.first())
        assertEquals(listOf(today.plusDays(7), far), days.takeLast(2))
    }

    @Test
    fun pagingAheadKeepsEveryDayOfTheShownWeekInTheList() {
        val today = LocalDate.of(2026, 11, 18)
        val state = UpcomingState(today)
        repeat(2) { state.page(forward = true) }
        val sunday = LocalDate.of(2026, 12, 6)
        assertEquals(sunday, state.days(emptySet()).last())
    }

    @Test
    fun theStripCannotPageBeforeThisWeekAndTodayComesHome() {
        val today = LocalDate.of(2026, 11, 18)
        val state = UpcomingState(today)
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
        val state = UpcomingState(today)
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
    fun theTopItemBelongsToTheNearestHeaderAboveIt() {
        assertEquals(mon, dayAt(days, count, 0))
        assertEquals(mon, dayAt(days, count, 2))
        assertEquals(mon.plusDays(1), dayAt(days, count, 3))
        assertEquals(mon.plusDays(2), dayAt(days, count, 5))
        assertEquals(mon.plusDays(3), dayAt(days, count, 6))
        assertEquals(mon.plusDays(3), dayAt(days, count, 40))
    }

    @Test
    fun followingADayBringsItsWeekIntoTheStrip() {
        val state = UpcomingState(LocalDate.of(2026, 11, 18))
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
        assertEquals(LocalDate.of(2026, 12, 5), state.days(emptySet()).last())
    }
}
