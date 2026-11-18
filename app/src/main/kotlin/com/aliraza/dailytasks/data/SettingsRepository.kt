package com.aliraza.dailytasks.data

import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.data.db.SettingsDao
import com.aliraza.dailytasks.data.db.toDomain
import com.aliraza.dailytasks.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the single settings row. A missing row means every setting is at its default. */
class SettingsRepository(private val dao: SettingsDao, private val onScheduleChanged: suspend () -> Unit) {
    fun observe(): Flow<Settings> = dao.observe().map { it?.toDomain() ?: Settings() }

    suspend fun current(): Settings = dao.get()?.toDomain() ?: Settings()

    /** Settings move reminders (default time, heads-up, overdue), so every change re-runs the engine. */
    suspend fun update(change: (Settings) -> Settings) {
        dao.upsert(change(current()).toEntity())
        onScheduleChanged()
    }
}
