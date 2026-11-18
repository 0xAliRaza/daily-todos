package com.aliraza.dailytasks.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.os.UserManagerCompat
import com.aliraza.dailytasks.AppContainer
import com.aliraza.dailytasks.DailyTasksApp
import com.aliraza.dailytasks.data.db.FiredBy
import kotlinx.coroutines.launch

private val Context.container: AppContainer get() = (applicationContext as DailyTasksApp).container

/** Runs [block] in the app scope while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend AppContainer.() -> Unit) {
    val pending = goAsync()
    val container = context.container
    container.appScope.launch {
        try {
            container.block()
        } finally {
            pending.finish()
        }
    }
}

/** Runs the engine, or the before-unlock fallback when the database is still locked. */
private fun BroadcastReceiver.runEngine(context: Context, firedBy: FiredBy) = runAsync(context) {
    if (UserManagerCompat.isUserUnlocked(context)) scheduler.run(firedBy) else beforeUnlock.run()
}

/** The single reminder alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) runEngine(context, FiredBy.ALARM)
    }

    companion object {
        const val ACTION = "com.aliraza.dailytasks.action.REMINDER_ALARM"
    }
}

/**
 * Events after which alarms must be re-armed: restarts (before and after unlock), app updates, clock
 * and time-zone changes, and the exact-alarm permission being granted on Android 12.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACTIONS) runEngine(context, FiredBy.CATCH_UP)
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            EXACT_ALARM_PERMISSION_CHANGED,
        )

        /** `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, sent on Android 12 and up. */
        const val EXACT_ALARM_PERMISSION_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}

/** Done and Snooze on a reminder. Before the first unlock nothing can be saved, so it is left in place. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val action = AndroidReminderPoster.Action.entries.firstOrNull { it.name == intent.action } ?: return
        if (!UserManagerCompat.isUserUnlocked(context)) return
        runAsync(context) { reminderActions.handle(taskId, action) }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}
