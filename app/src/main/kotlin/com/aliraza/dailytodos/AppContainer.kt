package com.aliraza.dailytodos

import android.content.Context
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.SubtaskRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.TimerRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.reminder.AndroidAlarmArmer
import com.aliraza.dailytodos.reminder.AndroidFocusSignals
import com.aliraza.dailytodos.reminder.AndroidReminderPoster
import com.aliraza.dailytodos.reminder.BeforeUnlockReminders
import com.aliraza.dailytodos.reminder.EndSignal
import com.aliraza.dailytodos.reminder.EngineTestReminder
import com.aliraza.dailytodos.reminder.FocusEngine
import com.aliraza.dailytodos.reminder.PrefsTestReminderStore
import com.aliraza.dailytodos.reminder.ReminderActions
import com.aliraza.dailytodos.reminder.ReminderScheduler
import com.aliraza.dailytodos.reminder.ScheduleMirror
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
    val lists: ListRepository by lazy { ListRepository(database.lists(), clock, onScheduleChanged) }
    val tasks: TaskRepository by lazy { TaskRepository(database, clock, settings, onScheduleChanged) }
    val subtasks: SubtaskRepository by lazy { SubtaskRepository(database.subtasks()) }
    val timers: TimerRepository by lazy {
        TimerRepository(database.timer(), database.focusSessions(), settings, clock, onScheduleChanged)
    }
    val backup: BackupRepository by lazy { BackupRepository(database, settings, clock, onScheduleChanged) }

    val alarmArmer by lazy { AndroidAlarmArmer(appContext) }
    val reminderPoster by lazy { AndroidReminderPoster(appContext, clock) }
    val focusSignals by lazy { AndroidFocusSignals(appContext, EndSignal(appContext, appScope)) }
    private val mirror by lazy {
        ScheduleMirror(File(appContext.createDeviceProtectedStorageContext().filesDir, ScheduleMirror.FILE_NAME))
    }

    private val testReminderStore by lazy { PrefsTestReminderStore(appContext) }

    val scheduler: ReminderScheduler by lazy {
        ReminderScheduler(
            tasks,
            lists,
            settings,
            database.reminderLog(),
            reminderPoster,
            alarmArmer,
            mirror,
            testReminderStore,
            clock,
            timer = FocusEngine(timers, tasks, focusSignals),
        )
    }
    val beforeUnlock by lazy { BeforeUnlockReminders(mirror, reminderPoster, alarmArmer, clock) }
    val testReminder by lazy { EngineTestReminder(testReminderStore, scheduler, clock) }
    val reminderActions by lazy { ReminderActions(tasks, reminderPoster, testReminder, clock) }
}
