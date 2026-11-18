package com.aliraza.dailytasks.ui.components

import com.aliraza.dailytasks.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DateToneTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun pastDatesAndPassedTimesAreOverdue() {
        assertEquals(DateTone.OVERDUE, dateTone(today.minusDays(1), today))
        assertEquals(DateTone.OVERDUE, dateTone(today, today, late = true))
    }

    @Test
    fun nearDatesGetTheirOwnTone() {
        assertEquals(DateTone.TODAY, dateTone(today, today))
        assertEquals(DateTone.TOMORROW, dateTone(today.plusDays(1), today))
        assertEquals(DateTone.THIS_WEEK, dateTone(today.plusDays(2), today))
        assertEquals(DateTone.THIS_WEEK, dateTone(today.plusDays(6), today))
    }

    @Test
    fun anOpenTaskPastItsTimeTodayIsOverdueButADoneOneIsNot() {
        val now = today.atTime(19, 0)
        val task = Task(
            id = "t",
            listId = "l",
            title = "Stretch",
            dueDate = today,
            dueTime = LocalTime.of(14, 33),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        assertEquals(DateTone.OVERDUE, task.dueTone(now))
        assertEquals(DateTone.TODAY, task.copy(dueTime = LocalTime.of(20, 0)).dueTone(now))
        assertEquals(DateTone.TODAY, task.copy(completedAt = Instant.EPOCH).dueTone(now))
        assertEquals(null, task.copy(dueDate = null).dueTone(now))
    }

    @Test
    fun aWeekOrMoreAwayIsLater() {
        assertEquals(DateTone.LATER, dateTone(today.plusDays(7), today))
        assertEquals(DateTone.LATER, dateTone(today.plusDays(40), today))
    }
}
