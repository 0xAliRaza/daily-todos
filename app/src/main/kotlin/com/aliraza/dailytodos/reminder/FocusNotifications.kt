package com.aliraza.dailytodos.reminder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.UserManagerCompat
import com.aliraza.dailytodos.DailyTodosApp
import com.aliraza.dailytodos.DeviceClock
import com.aliraza.dailytodos.MainActivity
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.ui.components.formatTime
import kotlinx.coroutines.launch

/**
 * The timer outside the app (PLAN.md section 6). A round's end posts one notification offering the next
 * step, and [EndSignal] sounds or buzzes a few times; nothing repeats and nothing starts by itself. While
 * a round runs, a quiet notification shows its task and countdown.
 */
class AndroidFocusSignals(private val context: Context, private val signal: EndSignal) : FocusSignals {
    private val manager = NotificationManagerCompat.from(context)

    override fun alert(ended: FocusTimer, title: String, settings: Settings) {
        if (!context.canPostNotifications()) return
        NotificationChannels.ensure(context)
        val focusEnded = ended.phase == FocusPhase.FOCUS
        val minutes = ended.roundLength?.toMinutes() ?: 0
        val builder = NotificationCompat.Builder(context, NotificationChannels.TIMER_END)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(if (focusEnded) R.color.notification_rest else R.color.notification_accent))
            .setContentTitle(
                if (focusEnded) {
                    context.getString(R.string.focus_alert_focus_done, minutes)
                } else {
                    context.getString(R.string.focus_alert_break_done)
                },
            )
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openFocus(ended.taskId))
            .addExtras(
                Bundle().apply {
                    putString(EXTRA_ENDED, ended.phase.name)
                    putString(EXTRA_TASK, ended.taskId)
                },
            )
        // Nothing runs while an alert is up, so it never offers Stop.
        if (focusEnded) {
            builder.addAction(0, context.getString(R.string.focus_start_break), action(FocusAction.START_BREAK))
            builder.addAction(0, context.getString(R.string.focus_skip_break), action(FocusAction.SKIP_BREAK))
        } else {
            builder.addAction(0, context.getString(R.string.focus_start), action(FocusAction.START_FOCUS, ended.taskId))
        }
        notify(ALERT_TAG, builder.build())
        signal.play(ended.phase, settings.timerSignal, asAlarm = settings.dndOverride)
    }

    override fun syncAlert(timer: FocusTimer) {
        val shown = context.getSystemService(NotificationManager::class.java)?.activeNotifications
            ?.firstOrNull { it.tag == ALERT_TAG && it.id == NOTIFICATION_ID } ?: return
        val extras = shown.notification.extras
        val ended = FocusPhase.entries.firstOrNull { it.name == extras.getString(EXTRA_ENDED) }
        if (ended == null || !FocusRounds.alertApplies(ended, extras.getString(EXTRA_TASK), timer)) {
            manager.cancel(ALERT_TAG, NOTIFICATION_ID)
        }
    }

    override fun showProgress(timer: FocusTimer, title: String, settings: Settings) {
        val running = timer.phase == FocusPhase.FOCUS || timer.phase == FocusPhase.BREAK
        if (!running || !context.canPostNotifications()) {
            manager.cancel(PROGRESS_TAG, NOTIFICATION_ID)
            return
        }
        NotificationChannels.ensure(context)
        val focusing = timer.phase == FocusPhase.FOCUS
        val builder = NotificationCompat.Builder(context, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(if (focusing) R.color.notification_accent else R.color.notification_rest))
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openFocus(timer.taskId))
            // Android 14 and later let an ongoing notification be swiped away; it comes straight back
            // while the round runs, so a running timer is never out of sight. Stop ends it.
            .setDeleteIntent(action(FocusAction.SHOW))
        val endsAt = timer.endsAt
        if (endsAt != null) {
            val at = formatTime(endsAt.atZone(DeviceClock.zone).toLocalTime(), settings.clockFormat)
            builder.setWhen(endsAt.toEpochMilli()).setShowWhen(true)
                .setUsesChronometer(true).setChronometerCountDown(true)
                .setContentText(
                    context.getString(
                        if (focusing) R.string.focus_progress_focus else R.string.focus_progress_break,
                        at,
                    ),
                )
                .addAction(0, context.getString(R.string.focus_pause), action(FocusAction.PAUSE))
        } else {
            val minutes = timer.pausedRemaining?.toMinutes() ?: 0
            builder.setContentText(context.getString(R.string.focus_paused_left, minutes.coerceAtLeast(1)))
                .addAction(0, context.getString(R.string.focus_resume), action(FocusAction.RESUME))
        }
        val stop = if (focusing) R.string.focus_stop else R.string.focus_end_break
        builder.addAction(
            0,
            context.getString(stop),
            action(if (focusing) FocusAction.STOP else FocusAction.SKIP_BREAK),
        )
        notify(PROGRESS_TAG, builder.build())
    }

    private fun notify(tag: String, notification: Notification) {
        try {
            manager.notify(tag, NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the post.
        }
    }

    /** Opens the timer of [taskId]; with no task, just the app. */
    private fun openFocus(taskId: String?): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_FOCUS, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: FocusAction, taskId: String? = null): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.ordinal,
        Intent(context, FocusActionReceiver::class.java).setAction(action.name).putExtra(EXTRA_TASK, taskId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val NOTIFICATION_ID = 2
        const val ALERT_TAG = "focus_alert"
        private const val PROGRESS_TAG = "focus_progress"

        /** On an alert: the phase that ended. On an alert and the Start focus action: the task. */
        const val EXTRA_ENDED = "focus_ended"
        const val EXTRA_TASK = "focus_task"
    }
}

/** [SHOW] is not a button: it re-posts the countdown after it was swiped away. */
enum class FocusAction { START_FOCUS, START_BREAK, SKIP_BREAK, PAUSE, RESUME, STOP, SHOW }

/** The timer's notification buttons. Each goes through the timer repository, which re-runs the scheduler. */
class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = FocusAction.entries.firstOrNull { it.name == intent.action } ?: return
        if (!UserManagerCompat.isUserUnlocked(context)) return
        val container = (context.applicationContext as DailyTodosApp).container
        val taskId = intent.getStringExtra(AndroidFocusSignals.EXTRA_TASK)
        val pending = goAsync()
        container.appScope.launch {
            try {
                val timers = container.timers
                when (action) {
                    FocusAction.START_FOCUS -> if (taskId != null) timers.start(taskId)

                    FocusAction.START_BREAK -> timers.startBreak()

                    FocusAction.SKIP_BREAK -> timers.skipBreak()

                    FocusAction.PAUSE -> timers.pause()

                    FocusAction.RESUME -> timers.resume()

                    FocusAction.STOP -> timers.stop()

                    // A scheduler run re-posts the countdown, with its task's title.
                    FocusAction.SHOW -> container.scheduler.run(FiredBy.CATCH_UP)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
