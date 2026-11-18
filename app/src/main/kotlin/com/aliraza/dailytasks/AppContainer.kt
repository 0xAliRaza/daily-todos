package com.aliraza.dailytasks

import android.content.Context
import com.aliraza.dailytasks.data.ListRepository
import com.aliraza.dailytasks.data.SettingsRepository
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.db.AppDatabase
import java.time.Clock

/**
 * Manual dependency injection: the single place where the app's long-lived objects are created.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val clock: Clock = Clock.systemDefaultZone()

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    /** Re-runs the reminder engine after a write. Wired to the scheduler in M3. */
    private val onScheduleChanged: suspend () -> Unit = {}

    val settings: SettingsRepository by lazy { SettingsRepository(database.settings(), onScheduleChanged) }
    val lists: ListRepository by lazy { ListRepository(database.lists()) }
    val tasks: TaskRepository by lazy { TaskRepository(database, clock, settings, onScheduleChanged) }
}
