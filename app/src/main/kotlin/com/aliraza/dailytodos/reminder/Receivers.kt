package com.aliraza.dailytodos.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.os.UserManagerCompat
import com.aliraza.dailytodos.AppContainer
import com.aliraza.dailytodos.DailyTodosApp
import com.aliraza.dailytodos.core.model.FiredBy
import kotlinx.coroutines.launch

private val Context.container: AppContainer get() = (applicationContext as DailyTodosApp).container

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
        const val ACTION = "com.aliraza.dailytodos.action.REMINDER_ALARM"
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

/** Complete and Snooze on a reminder. Before the first unlock nothing can be saved, so it is left in place. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val (taskId, action) = request(intent) ?: return
        if (!UserManagerCompat.isUserUnlocked(context)) return
        runAsync(context) { reminderActions.handle(taskId, action) }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"

        /**
         * The task and button an intent names, or null when it names neither: such as a button left in
         * the shade by an older version (its 1-hour snooze), which is then ignored.
         */
        fun request(intent: Intent): Pair<String, AndroidReminderPoster.Action>? {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return null
            val action = AndroidReminderPoster.Action.from(intent.action) ?: return null
            return taskId to action
        }
    }
}
