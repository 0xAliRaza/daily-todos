package com.aliraza.dailytasks.core.backup

import kotlinx.serialization.Serializable

/*
 * The backup file, version 1: one JSON document. Dates are ISO dates (2026-12-25), times are HH:mm wall
 * clock values, instants are ISO-8601 UTC timestamps (2026-11-18T09:30:00Z). The format is documented
 * in docs/backup-format.md; any change to these classes needs a new version and an upgrade path.
 */

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val settings: BackupSettings,
    val lists: List<BackupList>,
    val tasks: List<BackupTask>,
    val subtasks: List<BackupSubtask>,
    val reminderHistory: List<BackupReminder>,
)

@Serializable
data class BackupSettings(
    val defaultReminderTime: String,
    val undatedGetsTomorrow: Boolean,
    val dateOnlyGetsReminder: Boolean,
    val headsUpMinutes: Long,
    val overdueReminders: Boolean,
    val overdueReminderTime: String,
    val dndOverride: Boolean,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val breaksEnabled: Boolean,
    val timerSignal: String,
    val theme: String,
    val lastBackupAt: String?,
)

@Serializable
data class BackupList(val id: String, val name: String, val position: Long, val isDefault: Boolean)

@Serializable
data class BackupTask(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String,
    val priority: Int,
    val dueDate: String?,
    val dueTime: String?,
    val position: Long,
    val snoozedUntil: String?,
    val notifiedFor: String?,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupSubtask(val id: String, val taskId: String, val title: String, val done: Boolean, val position: Long)

@Serializable
data class BackupReminder(val taskId: String, val dueAt: String, val firedAt: String, val firedBy: String)
