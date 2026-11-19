package com.aliraza.dailytodos.data

import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.db.ListDao
import com.aliraza.dailytodos.data.db.ListEntity
import com.aliraza.dailytodos.data.db.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

/** Lists: the default "Daily" list first, then the user's own in their order. */
class ListRepository(
    private val dao: ListDao,
    private val clock: Clock,
    private val onScheduleChanged: suspend () -> Unit = {},
) {

    fun observe(): Flow<List<TaskList>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun all(): List<TaskList> = dao.all().map { it.toDomain() }

    /** Adds a list after the existing ones. */
    suspend fun create(name: String): TaskList {
        val list = ListEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            position = (dao.maxPosition() ?: 0) + 1,
            isDefault = false,
        )
        dao.insert(list)
        return list.toDomain()
    }

    suspend fun rename(id: String, name: String) {
        val list = dao.get(id) ?: return
        dao.update(listOf(list.copy(name = name.trim())))
    }

    /**
     * Soft-deletes a list with its tasks, or restores it for undo. Its tasks drop out of every view and
     * their reminders stop; both come back on restore. The default list can't be deleted.
     */
    suspend fun setDeleted(id: String, deleted: Boolean) {
        val list = dao.get(id)?.takeIf { !it.isDefault } ?: return
        dao.update(listOf(list.copy(deletedAt = if (deleted) clock.instant() else null)))
        onScheduleChanged()
    }
}
