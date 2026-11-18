package com.aliraza.dailytasks

import android.content.Context
import com.aliraza.dailytasks.core.model.FiredBy
import com.aliraza.dailytasks.data.BackupRepository
import com.aliraza.dailytasks.data.ListRepository
import com.aliraza.dailytasks.data.SettingsRepository
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.db.AppDatabase
import com.aliraza.dailytasks.reminder.AndroidAlarmArmer
import com.aliraza.dailytasks.reminder.AndroidReminderPoster
import com.aliraza.dailytasks.reminder.BeforeUnlockReminders
import com.aliraza.dailytasks.reminder.ReminderActions
import com.aliraza.dailytasks.reminder.ReminderScheduler
import com.aliraza.dailytasks.reminder.ScheduleMirror
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.time.Clock

/**
 * Manual dependency injection: the single place where the app's long-lived objects are created.
 *
 * Everything touching the database is lazy, because before the first unlock after a restart only the
 * device-protected parts ([beforeUnlock]) may be used.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val clock: Clock = DeviceClock
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    /** Every write that can move a reminder ends here, so the engine is the only scheduling path. */
    private val onScheduleChanged: suspend () -> Unit = { scheduler.run(FiredBy.CATCH_UP) }

    val settings: SettingsRepository by lazy { SettingsRepository(database.settings(), onScheduleChanged) }
    val lists: ListRepository by lazy { ListRepository(database.lists()) }
    val tasks: TaskRepository by lazy { TaskRepository(database, clock, settings, onScheduleChanged) }
    val backup: BackupRepository by lazy { BackupRepository(database, settings, clock, onScheduleChanged) }

    val alarmArmer by lazy { AndroidAlarmArmer(appContext) }
    val reminderPoster by lazy { AndroidReminderPoster(appContext) }
    private val mirror by lazy {
        ScheduleMirror(File(appContext.createDeviceProtectedStorageContext().filesDir, ScheduleMirror.FILE_NAME))
    }

    val scheduler: ReminderScheduler by lazy {
        ReminderScheduler(tasks, settings, database.reminderLog(), reminderPoster, alarmArmer, mirror, clock)
    }
    val beforeUnlock by lazy { BeforeUnlockReminders(mirror, reminderPoster, alarmArmer, clock) }
    val reminderActions by lazy { ReminderActions(tasks, reminderPoster, clock) }
}
