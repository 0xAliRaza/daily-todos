package com.aliraza.dailytasks.data

import androidx.room.withTransaction
import com.aliraza.dailytasks.core.model.NewTask
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.reminder.ReminderRules
import com.aliraza.dailytasks.data.db.AppDatabase
import com.aliraza.dailytasks.data.db.toDomain
import com.aliraza.dailytasks.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The only way tasks and their sub-tasks are written.
 *
 * Every write that can move a reminder calls [onScheduleChanged] afterwards, so the reminder engine
 * re-runs; UI code never talks to the scheduler directly.
 */
class TaskRepository(
    private val db: AppDatabase,
    private val clock: Clock,
    private val settings: SettingsRepository,
    private val onScheduleChanged: suspend () -> Unit,
) {
    private val tasks = db.tasks()
    private val lists = db.lists()

    private fun now(): Instant = clock.instant()

    private fun today(): LocalDate = LocalDate.now(clock)

    // Reads

    fun observeOpenDated(): Flow<List<Task>> = tasks.observeOpenDated().map { rows -> rows.map { it.toDomain() } }

    fun observeOpenInList(listId: String): Flow<List<Task>> =
        tasks.observeOpenInList(listId).map { rows -> rows.map { it.toDomain() } }

    /**
     * What Today shows as done: tasks ticked off since midnight on [day] that were due that day or
     * earlier. Undated tasks from other lists stay out, as they never showed in Today.
     */
    fun observeCompletedOn(day: LocalDate): Flow<List<Task>> =
        tasks.observeCompletedSince(day.atStartOfDay(clock.zone).toInstant()).map { rows ->
            rows.map { it.toDomain() }.filter { task -> task.dueDate?.isAfter(day) == false }
        }

    /** Open tasks per list id; lists without any are missing from the map. */
    fun observeOpenCounts(): Flow<Map<String, Int>> =
        tasks.observeOpenCounts().map { rows -> rows.associate { it.listId to it.open } }

    /** A list's done tasks, the most recently ticked first. */
    fun observeCompletedInList(listId: String): Flow<List<Task>> =
        tasks.observeCompletedInList(listId).map { rows -> rows.map { it.toDomain() } }

    fun observeTask(id: String): Flow<Task?> = tasks.observe(id).map { it?.toDomain() }

    /** Tasks whose title or notes contain [query], case-insensitively; `%` and `_` match literally. */
    fun search(query: String): Flow<List<Task>> {
        val escaped = query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return tasks.search("%$escaped%").map { rows -> rows.map { it.toDomain() } }
    }

    suspend fun openDated(): List<Task> = tasks.openDated().map { it.toDomain() }

    suspend fun task(id: String): Task? = tasks.get(id)?.toDomain()

    // Task writes

    /** Creates a task at the bottom of its list and returns it. */
    suspend fun addTask(draft: NewTask): Task {
        val created = db.withTransaction {
            val list = draft.listId?.let { lists.get(it) }?.takeIf { it.deletedAt == null } ?: lists.defaultList()
            val settings = settings.current()
            val now = now()
            val task = Task(
                id = UUID.randomUUID().toString(),
                listId = list.id,
                title = draft.title.trim(),
                notes = draft.notes,
                priority = draft.priority,
                dueDate = draft.effectiveDueDate(list.isDefault, settings, today()),
                dueTime = draft.dueTime,
                position = (tasks.maxPosition(list.id) ?: -1) + 1,
                createdAt = now,
                updatedAt = now,
            )
            ReminderRules.rescheduled(task, settings, now, clock.zone).also { tasks.insert(it.toEntity()) }
        }
        onScheduleChanged()
        return created
    }

    /**
     * Applies [change] to a task. A changed date or time starts its reminders afresh (see
     * [ReminderRules.rescheduled]); moving it to another list puts it at the bottom there.
     */
    suspend fun updateTask(id: String, change: (Task) -> Task) {
        updateTasks(listOf(id), change)
    }

    /**
     * Bulk edit, used by multi-select (re-date, priority). One transaction, one reschedule. Returns the
     * tasks as they were just before the change, read in the same transaction, for undo.
     */
    suspend fun updateTasks(ids: Collection<String>, change: (Task) -> Task): List<Task> {
        val before = db.withTransaction {
            val settings = settings.current()
            val now = now()
            val rows = tasks.getMany(ids)
            val updated = rows.map { row ->
                val before = row.toDomain()
                val after = change(before).copy(id = before.id, updatedAt = now)
                val moved = if (after.listId != before.listId) {
                    after.copy(position = (tasks.maxPosition(after.listId) ?: -1) + 1)
                } else {
                    after
                }
                val redated = moved.dueDate != before.dueDate || moved.dueTime != before.dueTime
                if (redated) ReminderRules.rescheduled(moved, settings, now, clock.zone) else moved
            }
            tasks.update(updated.map { it.toEntity() })
            rows.map { it.toDomain() }
        }
        onScheduleChanged()
        return before
    }

    /** Completes or, for undo, reopens tasks. */
    suspend fun setCompleted(ids: Collection<String>, completed: Boolean) {
        val now = now()
        tasks.setCompleted(ids, if (completed) now else null, now)
        onScheduleChanged()
    }

    /** Soft-deletes or, for undo, restores tasks. Deleted tasks are purged by [purgeDeleted]. */
    suspend fun setDeleted(ids: Collection<String>, deleted: Boolean) {
        val now = now()
        tasks.setDeleted(ids, if (deleted) now else null, now)
        onScheduleChanged()
    }

    suspend fun snooze(id: String, until: Instant) {
        updateTask(id) { it.copy(snoozedUntil = until) }
    }

    /** Called by the reminder engine after posting; does not re-trigger the engine. */
    suspend fun markNotified(id: String, instant: Instant) {
        tasks.setNotifiedFor(id, instant)
    }

    /** Stores a manual order: [orderedIds] get positions 0, 1, 2… in their list. */
    suspend fun reorder(orderedIds: List<String>) {
        db.withTransaction {
            val byId = tasks.getMany(orderedIds).associateBy { it.id }
            val now = now()
            tasks.update(
                orderedIds.mapIndexedNotNull { index, id ->
                    byId[id]?.copy(position = index.toLong(), updatedAt = now)
                },
            )
        }
    }

    /** Removes for good what was soft-deleted longer ago than [age]; undo is no longer possible by then. */
    suspend fun purgeDeleted(age: Duration = UNDO_WINDOW) {
        val before = now().minus(age)
        db.withTransaction {
            tasks.purgeDeletedBefore(before)
            // A deleted list takes its tasks with it; they go first so the list can.
            tasks.purgeInListsDeletedBefore(before)
            lists.purgeDeletedBefore(before)
        }
    }

    companion object {
        /** How long a soft-deleted item can still be restored. */
        val UNDO_WINDOW: Duration = Duration.ofDays(1)
    }
}
