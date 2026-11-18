package com.aliraza.dailytasks.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    /** Open tasks that have a date, from every list: the source of Today, Upcoming and the scheduler. */
    @Query(
        "SELECT tasks.* FROM tasks JOIN lists ON lists.id = tasks.list_id " +
            "WHERE tasks.due_date IS NOT NULL AND tasks.completed_at IS NULL AND tasks.deleted_at IS NULL " +
            "AND lists.deleted_at IS NULL",
    )
    fun observeOpenDated(): Flow<List<TaskEntity>>

    @Query(
        "SELECT tasks.* FROM tasks JOIN lists ON lists.id = tasks.list_id " +
            "WHERE tasks.due_date IS NOT NULL AND tasks.completed_at IS NULL AND tasks.deleted_at IS NULL " +
            "AND lists.deleted_at IS NULL",
    )
    suspend fun openDated(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE list_id = :listId AND completed_at IS NULL AND deleted_at IS NULL")
    fun observeOpenInList(listId: String): Flow<List<TaskEntity>>

    /** Title or notes contain [pattern] (a LIKE pattern); done tasks included, deleted ones not. */
    @Query(
        "SELECT tasks.* FROM tasks JOIN lists ON lists.id = tasks.list_id " +
            "WHERE tasks.deleted_at IS NULL AND lists.deleted_at IS NULL " +
            "AND (tasks.title LIKE :pattern ESCAPE '\\' OR tasks.notes LIKE :pattern ESCAPE '\\') " +
            "ORDER BY tasks.completed_at IS NOT NULL, tasks.updated_at DESC",
    )
    fun search(pattern: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getMany(ids: Collection<String>): List<TaskEntity>

    @Query("SELECT MAX(position) FROM tasks WHERE list_id = :listId")
    suspend fun maxPosition(listId: String): Long?

    @Insert
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET completed_at = :completedAt, updated_at = :now WHERE id IN (:ids)")
    suspend fun setCompleted(ids: Collection<String>, completedAt: Instant?, now: Instant)

    @Query("UPDATE tasks SET deleted_at = :deletedAt, updated_at = :now WHERE id IN (:ids)")
    suspend fun setDeleted(ids: Collection<String>, deletedAt: Instant?, now: Instant)

    @Query("UPDATE tasks SET notified_for = :instant WHERE id = :id")
    suspend fun setNotifiedFor(id: String, instant: Instant)

    @Query("DELETE FROM tasks WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeDeletedBefore(before: Instant): Int
}

@Dao
interface ListDao {
    @Query("SELECT * FROM lists WHERE deleted_at IS NULL ORDER BY is_default DESC, position")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE deleted_at IS NULL ORDER BY is_default DESC, position")
    suspend fun all(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE is_default = 1")
    suspend fun defaultList(): ListEntity

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun get(id: String): ListEntity?

    @Query("SELECT MAX(position) FROM lists")
    suspend fun maxPosition(): Long?

    @Insert
    suspend fun insert(list: ListEntity)

    @Update
    suspend fun update(lists: List<ListEntity>)

    @Query("DELETE FROM lists WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeDeletedBefore(before: Instant): Int
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position")
    fun observeForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT MAX(position) FROM subtasks WHERE task_id = :taskId")
    suspend fun maxPosition(taskId: String): Long?

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLE_ROW}")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLE_ROW}")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface ReminderLogDao {
    @Insert
    suspend fun insert(entry: ReminderLogEntity)

    /** Keeps the newest [keep] rows. */
    @Query(
        "DELETE FROM reminder_log WHERE id NOT IN " +
            "(SELECT id FROM reminder_log ORDER BY fired_at DESC, id DESC LIMIT :keep)",
    )
    suspend fun trim(keep: Int)

    @Query("SELECT * FROM reminder_log ORDER BY fired_at DESC, id DESC")
    fun observeAll(): Flow<List<ReminderLogEntity>>
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_state WHERE id = ${SettingsEntity.SINGLE_ROW}")
    fun observe(): Flow<TimerStateEntity?>

    @Query("SELECT * FROM timer_state WHERE id = ${SettingsEntity.SINGLE_ROW}")
    suspend fun get(): TimerStateEntity?

    @Upsert
    suspend fun upsert(state: TimerStateEntity)
}
