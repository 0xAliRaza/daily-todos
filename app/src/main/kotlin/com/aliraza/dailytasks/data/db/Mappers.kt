package com.aliraza.dailytasks.data.db

import com.aliraza.dailytasks.core.model.HeadsUp
import com.aliraza.dailytasks.core.model.Priority
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.Subtask
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.model.TaskList
import com.aliraza.dailytasks.core.model.ThemeMode
import com.aliraza.dailytasks.core.model.TimerSignal

/** Conversions between table rows and the domain model in :core. */

fun TaskEntity.toDomain() = Task(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    priority = Priority.fromLevel(priority),
    dueDate = dueDate,
    dueTime = dueTime,
    position = position,
    snoozedUntil = snoozedUntil,
    notifiedFor = notifiedFor,
    completedAt = completedAt,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    priority = priority.level,
    dueDate = dueDate,
    dueTime = dueTime,
    position = position,
    snoozedUntil = snoozedUntil,
    notifiedFor = notifiedFor,
    completedAt = completedAt,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ListEntity.toDomain() = TaskList(id = id, name = name, position = position, isDefault = isDefault)

fun SubtaskEntity.toDomain() = Subtask(id = id, taskId = taskId, title = title, done = done, position = position)

fun Subtask.toEntity() = SubtaskEntity(id = id, taskId = taskId, title = title, done = done, position = position)

fun SettingsEntity.toDomain() = Settings(
    defaultReminderTime = defaultReminderTime,
    undatedGetsTomorrow = undatedGetsTomorrow,
    dateOnlyGetsReminder = dateOnlyGetsReminder,
    headsUp = HeadsUp.fromMinutes(headsUpMinutes),
    overdueReminders = overdueReminders,
    overdueReminderTime = overdueReminderTime,
    dndOverride = dndOverride,
    focusMinutes = focusMinutes,
    breakMinutes = breakMinutes,
    breaksEnabled = breaksEnabled,
    timerSignal = enumOrDefault(timerSignal, TimerSignal.BOTH),
    theme = enumOrDefault(theme, ThemeMode.SYSTEM),
    lastBackupAt = lastBackupAt,
)

fun Settings.toEntity() = SettingsEntity(
    defaultReminderTime = defaultReminderTime,
    undatedGetsTomorrow = undatedGetsTomorrow,
    dateOnlyGetsReminder = dateOnlyGetsReminder,
    headsUpMinutes = headsUp.minutes,
    overdueReminders = overdueReminders,
    overdueReminderTime = overdueReminderTime,
    dndOverride = dndOverride,
    focusMinutes = focusMinutes,
    breakMinutes = breakMinutes,
    breaksEnabled = breaksEnabled,
    timerSignal = timerSignal.name,
    theme = theme.name,
    lastBackupAt = lastBackupAt,
)

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
