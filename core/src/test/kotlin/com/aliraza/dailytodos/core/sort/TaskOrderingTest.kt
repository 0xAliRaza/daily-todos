package com.aliraza.dailytodos.core.sort

import com.aliraza.dailytodos.core.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class TaskOrderingTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val ordering = TaskOrdering(mapOf("daily" to 0L, "groceries" to 1L))

    @Test
    fun todayShowsOverdueThenTimedByTimeThenTheRestInManualOrder() {
        val tasks = listOf(
            task(id = "rest-groceries", date = today, listId = "groceries", position = 0),
            task(id = "timed-16", date = today, time = LocalTime.of(16, 0)),
            task(id = "overdue-yesterday", date = today.minusDays(1)),
            task(id = "rest-daily-2", date = today, position = 2),
            task(id = "timed-09", date = today, time = LocalTime.of(9, 0)),
            task(id = "overdue-last-week", date = today.minusDays(7), time = LocalTime.of(8, 0)),
            task(id = "rest-daily-1", date = today, position = 1),
            task(id = "tomorrow", date = today.plusDays(1)),
            task(id = "undated"),
            task(id = "done", date = today, completedAt = Instant.EPOCH),
        )
        val sections = ordering.today(tasks, today)
        assertEquals(listOf("overdue-last-week", "overdue-yesterday"), sections.overdue.map { it.id })
        assertEquals(listOf("timed-09", "timed-16"), sections.timed.map { it.id })
        assertEquals(listOf("rest-daily-1", "rest-daily-2", "rest-groceries"), sections.rest.map { it.id })
    }

    @Test
    fun listKeepsManualOrderWithTiesBrokenByCreation() {
        val tasks = listOf(
            task(id = "b", position = 1, createdAt = Instant.ofEpochSecond(2)),
            task(id = "c", position = 1, createdAt = Instant.ofEpochSecond(3)),
            task(id = "a", position = 0),
        )
        assertEquals(listOf("a", "b", "c"), ordering.list(tasks).map { it.id })
    }

    @Test
    fun upcomingDayPutsTimedTasksFirst() {
        val tasks = listOf(
            task(id = "untimed", date = today, position = 0),
            task(id = "late", date = today, time = LocalTime.of(20, 0)),
            task(id = "early", date = today, time = LocalTime.of(7, 0)),
        )
        assertEquals(listOf("early", "late", "untimed"), ordering.day(tasks).map { it.id })
    }
}
