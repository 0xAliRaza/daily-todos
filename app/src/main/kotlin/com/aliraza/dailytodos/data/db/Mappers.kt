package com.aliraza.dailytodos.data.db

import com.aliraza.dailytodos.core.focus.FocusSession
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.core.model.WeekStart
import java.time.Duration

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

fun TaskList.toEntity() = ListEntity(id = id, name = name, position = position, isDefault = isDefault)

fun ReminderLogEntity.toDomain() = ReminderLogEntry(
    taskId = taskId,
    dueAt = dueAt,
    firedAt = firedAt,
    firedBy = firedBy,
)

fun ReminderLogEntry.toEntity() = ReminderLogEntity(
    taskId = taskId,
    dueAt = dueAt,
    firedAt = firedAt,
    firedBy = firedBy,
)

fun FocusSessionEntity.toDomain() = FocusSession(
    taskId = taskId,
    startedAt = startedAt,
    endedAt = endedAt,
    focused = Duration.ofMillis(focusedMs),
)

fun FocusSession.toEntity() = FocusSessionEntity(
    taskId = taskId,
    startedAt = startedAt,
    endedAt = endedAt,
    focusedMs = focused.toMillis(),
)

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
    focusCustomMinutes = focusCustomMinutes,
    breakCustomMinutes = breakCustomMinutes,
    timerSignal = enumOrDefault(timerSignal, TimerSignal.BOTH),
    theme = enumOrDefault(theme, ThemeMode.SYSTEM),
    clockFormat = enumOrDefault(clockFormat, ClockFormat.H12),
    weekStart = enumOrDefault(weekStart, WeekStart.PHONE),
    haptics = haptics,
    keepScreenOn = keepScreenOn,
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
    focusCustomMinutes = focusCustomMinutes,
    breakCustomMinutes = breakCustomMinutes,
    timerSignal = timerSignal.name,
    theme = theme.name,
    clockFormat = clockFormat.name,
    weekStart = weekStart.name,
    haptics = haptics,
    keepScreenOn = keepScreenOn,
    lastBackupAt = lastBackupAt,
)

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
