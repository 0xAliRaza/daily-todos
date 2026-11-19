package com.aliraza.dailytodos.data

import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.data.db.SubtaskDao
import com.aliraza.dailytodos.data.db.toDomain
import com.aliraza.dailytodos.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** How far a task's checklist has got: [done] of [total] sub-tasks ticked. */
data class SubtaskProgress(val done: Int, val total: Int)

/** The one level of checkboxes inside a task. Sub-tasks carry no dates, so no write moves a reminder. */
class SubtaskRepository(private val subtasks: SubtaskDao) {

    fun observe(taskId: String): Flow<List<Subtask>> =
        subtasks.observeForTask(taskId).map { rows -> rows.map { it.toDomain() } }

    /** Progress for every task that has sub-tasks, keyed by task id, so rows can show "1/3". */
    fun observeProgress(): Flow<Map<String, SubtaskProgress>> = subtasks.observeProgress().map { rows ->
        rows.associate { it.taskId to SubtaskProgress(it.done, it.total) }
    }

    suspend fun add(taskId: String, title: String): Subtask {
        val subtask = Subtask(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            title = title.trim(),
            done = false,
            position = (subtasks.maxPosition(taskId) ?: -1) + 1,
        )
        subtasks.upsert(subtask.toEntity())
        return subtask
    }

    suspend fun update(subtask: Subtask) {
        subtasks.upsert(subtask.toEntity())
    }

    suspend fun delete(id: String) {
        subtasks.delete(id)
    }
}
