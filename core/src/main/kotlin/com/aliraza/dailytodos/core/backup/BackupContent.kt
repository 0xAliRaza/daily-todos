package com.aliraza.dailytodos.core.backup

import com.aliraza.dailytodos.core.focus.FocusSession
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList

/**
 * Everything a backup holds, in domain types. Soft-deleted items are not part of it, and neither is the
 * running focus timer, which only makes sense on the phone it runs on.
 */
data class BackupContent(
    val settings: Settings,
    val lists: List<TaskList>,
    val tasks: List<Task>,
    val subtasks: List<Subtask>,
    val reminderHistory: List<ReminderLogEntry>,
    val focusSessions: List<FocusSession> = emptyList(),
)
