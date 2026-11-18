package com.aliraza.dailytasks.core.reminder

import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlanTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = today) =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant()

    @Test
    fun fiveRemindersInTheSameMinuteAreFiveNotifications() {
        val tasks = (1..5).map { task(id = "t$it", date = today, time = LocalTime.of(16, 0)) }
        val plan = ReminderPlan.compute(tasks, Settings(), at(16, 0), zone)
        assertEquals(tasks.map { it.id }.toSet(), plan.due.map { it.task.id }.toSet())
        assertEquals(at(9, date = today.plusDays(1)), plan.nextAt)
    }

    @Test
    fun nextAlarmIsTheEarliestFutureReminderAfterPostingWhatIsDue() {
        val tasks = listOf(
            task(id = "missed", date = today, time = LocalTime.of(11, 0)),
            task(id = "later", date = today, time = LocalTime.of(18, 0)),
            task(id = "sooner", date = today, time = LocalTime.of(14, 30)),
            task(id = "undated"),
        )
        val plan = ReminderPlan.compute(tasks, Settings(), at(12, 0), zone)
        assertEquals(listOf("missed"), plan.due.map { it.task.id })
        assertEquals(at(11, 0), plan.due.single().at)
        assertEquals(listOf("sooner", "later", "missed"), plan.upcoming.map { it.task.id })
        assertEquals(at(14, 30), plan.nextAt)
    }

    @Test
    fun nothingPendingMeansNoAlarm() {
        val plan = ReminderPlan.compute(listOf(task()), Settings(), at(12, 0), zone)
        assertEquals(emptyList<ReminderPlan.Reminder>(), plan.due)
        assertNull(plan.nextAt)
    }
}
