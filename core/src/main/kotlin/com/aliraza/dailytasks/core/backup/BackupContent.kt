package com.aliraza.dailytasks.core.backup

import com.aliraza.dailytasks.core.focus.FocusSession
import com.aliraza.dailytasks.core.model.ReminderLogEntry
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.Subtask
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.model.TaskList

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
