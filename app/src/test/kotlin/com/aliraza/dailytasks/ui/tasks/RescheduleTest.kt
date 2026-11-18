package com.aliraza.dailytasks.ui.tasks

import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RescheduleTest {

    private val wednesday = LocalDate.of(2026, 11, 18)
    private val task = Task(
        id = "t",
        listId = "l",
        title = "Call bank",
        dueDate = wednesday,
        dueTime = LocalTime.of(16, 0),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun movingKeepsTheTime() {
        val moved = task.movedTo(wednesday.plusDays(2))
        assertEquals(wednesday.plusDays(2), moved.dueDate)
        assertEquals(LocalTime.of(16, 0), moved.dueTime)
    }

    @Test
    fun noDateAlsoDropsTheTime() {
        val moved = task.movedTo(null)
        assertEquals(null, moved.dueDate)
        assertEquals(null, moved.dueTime)
    }

    @Test
    fun theQuickDaysAreTodayTomorrowAndNextMonday() {
        assertEquals(
            listOf(
                QuickDate(R.string.day_today, wednesday),
                QuickDate(R.string.day_tomorrow, LocalDate.of(2026, 11, 19)),
                QuickDate(R.string.date_next_week, LocalDate.of(2026, 11, 23)),
            ),
            quickDates(wednesday),
        )
    }
}
