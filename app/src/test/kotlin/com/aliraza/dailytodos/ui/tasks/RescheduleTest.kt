package com.aliraza.dailytodos.ui.tasks

import com.aliraza.dailytodos.core.model.Task
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
}
