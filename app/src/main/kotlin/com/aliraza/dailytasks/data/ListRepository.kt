package com.aliraza.dailytasks.data

import com.aliraza.dailytasks.core.model.TaskList
import com.aliraza.dailytasks.data.db.ListDao
import com.aliraza.dailytasks.data.db.ListEntity
import com.aliraza.dailytasks.data.db.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Lists: the default "Daily" list first, then the user's own in their order. */
class ListRepository(private val dao: ListDao) {

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
}
