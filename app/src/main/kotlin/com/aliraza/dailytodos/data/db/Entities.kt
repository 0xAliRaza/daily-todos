package com.aliraza.dailytodos.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aliraza.dailytodos.core.model.FiredBy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Table rows. Dates are ISO text, times `HH:mm`, instants epoch milliseconds (see [Converters]). */

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Long,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    /** Soft delete so deleting a list can be undone. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(entity = ListEntity::class, parentColumns = ["id"], childColumns = ["list_id"]),
    ],
    indices = [Index("list_id"), Index("due_date")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val title: String,
    val notes: String,
    val priority: Int,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate?,
    @ColumnInfo(name = "due_time") val dueTime: LocalTime?,
    val position: Long,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Instant?,
    @ColumnInfo(name = "notified_for") val notifiedFor: Instant?,
    @ColumnInfo(name = "completed_at") val completedAt: Instant?,
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_id")],
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val title: String,
    val done: Boolean,
    val position: Long,
)

/** How many open tasks a list holds, as [TaskDao.observeOpenCounts] reads them. */
data class ListCountRow(val listId: String, val open: Int)

/** A task's sub-task counts, as [SubtaskDao.observeProgress] reads them. */
data class SubtaskProgressRow(val taskId: String, val total: Int, val done: Int)

/** One posted reminder, for the Reminder health history. Deliberately holds no title. */
@Entity(tableName = "reminder_log", indices = [Index("fired_at")])
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "due_at") val dueAt: Instant,
    @ColumnInfo(name = "fired_at") val firedAt: Instant,
    @ColumnInfo(name = "fired_by") val firedBy: FiredBy,
)

/** The single settings row; [id] is always [SINGLE_ROW]. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "default_reminder_time") val defaultReminderTime: LocalTime,
    @ColumnInfo(name = "undated_gets_tomorrow") val undatedGetsTomorrow: Boolean,
    @ColumnInfo(name = "date_only_gets_reminder") val dateOnlyGetsReminder: Boolean,
    @ColumnInfo(name = "heads_up_minutes") val headsUpMinutes: Long,
    @ColumnInfo(name = "overdue_reminders") val overdueReminders: Boolean,
    @ColumnInfo(name = "overdue_reminder_time") val overdueReminderTime: LocalTime,
    @ColumnInfo(name = "dnd_override") val dndOverride: Boolean,
    @ColumnInfo(name = "focus_minutes") val focusMinutes: Int,
    @ColumnInfo(name = "break_minutes") val breakMinutes: Int,
    @ColumnInfo(name = "focus_custom_minutes", defaultValue = "90") val focusCustomMinutes: Int,
    @ColumnInfo(name = "break_custom_minutes", defaultValue = "20") val breakCustomMinutes: Int,
    @ColumnInfo(name = "timer_signal") val timerSignal: String,
    val theme: String,
    @ColumnInfo(name = "clock_format", defaultValue = "'H12'") val clockFormat: String,
    @ColumnInfo(name = "week_start", defaultValue = "'PHONE'") val weekStart: String,
    @ColumnInfo(defaultValue = "1") val haptics: Boolean,
    @ColumnInfo(name = "keep_screen_on", defaultValue = "0") val keepScreenOn: Boolean,
    @ColumnInfo(name = "last_backup_at") val lastBackupAt: Instant?,
) {
    companion object {
        const val SINGLE_ROW = 1
    }
}

/**
 * The focus timer, persisted so it survives the process being killed. One row, like settings. The task
 * has no foreign key: the timer engine stops a timer whose task is done or gone.
 */
@Entity(tableName = "timer_state")
data class TimerStateEntity(
    @PrimaryKey val id: Int = SettingsEntity.SINGLE_ROW,
    val phase: String,
    @ColumnInfo(name = "ends_at") val endsAt: Instant?,
    @ColumnInfo(name = "task_id") val taskId: String?,
    @ColumnInfo(name = "paused_remaining_ms") val pausedRemainingMs: Long?,
    @ColumnInfo(name = "round_ms") val roundMs: Long?,
)

/**
 * One stretch of focus, written when it ends. Deleting a task for good keeps its time in the totals:
 * [taskId] becomes null.
 */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("task_id"), Index("ended_at")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: String?,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "ended_at") val endedAt: Instant,
    @ColumnInfo(name = "focused_ms") val focusedMs: Long,
)

/** A task's focus so far, as [FocusSessionDao.observeForTask] reads it. */
data class TaskFocusRow(val focusedMs: Long, val sessions: Int)

/** A task's id and title, as [TaskDao.observeKeptTitles] reads them. */
data class TaskTitleRow(val id: String, val title: String)
