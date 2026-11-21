package com.aliraza.dailytodos.ui.settings

import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.reminder.TestReminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** The reminder history names each reminder's task from the live titles; the log holds no names. */
class HistoryTitlesTest {
    private val at = Instant.parse("2026-11-21T15:00:00Z")

    private fun entry(taskId: String) = ReminderLogEntry(taskId, at, at, FiredBy.ALARM)

    @Test
    fun aKeptTaskShowsItsTitleAsItIsNow() {
        val items = historyItems(listOf(entry("a")), mapOf("a" to "Call bank"))
        assertEquals(listOf(HistoryItem(entry("a"), HistoryTitle.Task("Call bank"))), items)
    }

    @Test
    fun aTaskWithNoTitleLeftIsDeleted() {
        val items = historyItems(listOf(entry("gone")), mapOf("a" to "Call bank"))
        assertEquals(HistoryTitle.Deleted, items.single().title)
    }

    @Test
    fun theTestReminderIsNamedAsTheTest() {
        assertEquals(HistoryTitle.Test, historyItems(listOf(entry(TestReminder.TASK_ID)), emptyMap()).single().title)
    }

    @Test
    fun theOrderOfTheLogIsKept() {
        val items = historyItems(listOf(entry("b"), entry("a")), mapOf("a" to "A", "b" to "B"))
        assertEquals(listOf("b", "a"), items.map { it.entry.taskId })
    }
}
