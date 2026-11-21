package com.aliraza.dailytodos.ui.settings

import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.reminder.TestReminder

/** Who a logged reminder was for, as the history shows it. */
sealed interface HistoryTitle {
    /** A task that still exists, under its name as it is now. */
    data class Task(val title: String) : HistoryTitle

    /** The task is gone; the log never held its name. */
    data object Deleted : HistoryTitle

    /** The test sent from Reminder health, which belongs to no task. */
    data object Test : HistoryTitle
}

/** One row of the reminder history: the log entry and its title. */
data class HistoryItem(val entry: ReminderLogEntry, val title: HistoryTitle)

/** Joins each of [entries] to its task's title in [titles] (id to title), in the entries' order. */
fun historyItems(entries: List<ReminderLogEntry>, titles: Map<String, String>): List<HistoryItem> = entries.map {
    val title = when (it.taskId) {
        TestReminder.TASK_ID -> HistoryTitle.Test
        else -> titles[it.taskId]?.let(HistoryTitle::Task) ?: HistoryTitle.Deleted
    }
    HistoryItem(it, title)
}
