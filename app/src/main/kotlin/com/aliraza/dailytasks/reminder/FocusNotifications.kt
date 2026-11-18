package com.aliraza.dailytasks.reminder

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.UserManagerCompat
import com.aliraza.dailytasks.DailyTasksApp
import com.aliraza.dailytasks.MainActivity
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.TimerSignal
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * The timer outside the app (PLAN.md section 6). The end alert is "insistent": it repeats until the user
 * acts or dismisses it, and the system removes it after [ALERT_CAP] so an unattended phone goes quiet.
 */
class AndroidFocusSignals(private val context: Context) : FocusSignals {
    private val manager = NotificationManagerCompat.from(context)

    override fun alert(ended: FocusPhase, signal: TimerSignal) {
        if (!context.canPostNotifications()) return
        NotificationChannels.ensure(context)
        val channel = when (signal) {
            TimerSignal.SOUND -> NotificationChannels.TIMER_SOUND
            TimerSignal.VIBRATE -> NotificationChannels.TIMER_VIBRATE
            TimerSignal.BOTH -> NotificationChannels.TIMER_BOTH
        }
        val focusEnded = ended == FocusPhase.FOCUS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.getString(if (focusEnded) R.string.focus_alert_focus_done else R.string.focus_alert_break_done),
            )
            .setContentText(
                context.getString(if (focusEnded) R.string.focus_alert_focus_body else R.string.focus_alert_break_body),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setTimeoutAfter(ALERT_CAP.toMillis())
            .setAutoCancel(true)
            .setContentIntent(openFocus())
        if (focusEnded) {
            builder.addAction(0, context.getString(R.string.focus_skip_break), action(FocusAction.SKIP_BREAK))
        } else {
            builder.addAction(0, context.getString(R.string.focus_start_next), action(FocusAction.NEXT))
        }
        builder.addAction(0, context.getString(R.string.focus_stop), action(FocusAction.STOP))
        val notification = builder.build().apply { flags = flags or Notification.FLAG_INSISTENT }
        notify(ALERT_TAG, notification)
    }

    override fun clearAlert() {
        manager.cancel(ALERT_TAG, NOTIFICATION_ID)
    }

    override fun showProgress(timer: FocusTimer) {
        val running = timer.phase == FocusPhase.FOCUS || timer.phase == FocusPhase.BREAK
        if (!running || !context.canPostNotifications()) {
            manager.cancel(PROGRESS_TAG, NOTIFICATION_ID)
            return
        }
        NotificationChannels.ensure(context)
        val title = context.getString(
            if (timer.phase == FocusPhase.FOCUS) R.string.focus_progress_focus else R.string.focus_progress_break,
            timer.round,
        )
        val builder = NotificationCompat.Builder(context, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openFocus())
        val endsAt = timer.endsAt
        if (endsAt != null) {
            builder.setWhen(
                endsAt.toEpochMilli(),
            ).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.focus_pause), action(FocusAction.PAUSE))
        } else {
            val minutes = timer.pausedRemaining?.toMinutes() ?: 0
            builder.setContentText(context.getString(R.string.focus_paused_left, minutes.coerceAtLeast(1)))
                .addAction(0, context.getString(R.string.focus_resume), action(FocusAction.RESUME))
        }
        builder.addAction(0, context.getString(R.string.focus_stop), action(FocusAction.STOP))
        notify(PROGRESS_TAG, builder.build())
    }

    private fun notify(tag: String, notification: Notification) {
        try {
            manager.notify(tag, NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the post.
        }
    }

    private fun openFocus(): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: FocusAction): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.ordinal,
        Intent(context, FocusActionReceiver::class.java).setAction(action.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** The end alert stops by itself after this long (PLAN.md section 11, planner default). */
        val ALERT_CAP: Duration = Duration.ofSeconds(60)
        const val NOTIFICATION_ID = 2
        private const val ALERT_TAG = "focus_alert"
        private const val PROGRESS_TAG = "focus_progress"
    }
}

enum class FocusAction { NEXT, SKIP_BREAK, PAUSE, RESUME, STOP }

/** The timer's notification buttons. Each goes through the timer repository, which re-runs the scheduler. */
class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = FocusAction.entries.firstOrNull { it.name == intent.action } ?: return
        if (!UserManagerCompat.isUserUnlocked(context)) return
        val container = (context.applicationContext as DailyTasksApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                val timers = container.timers
                when (action) {
                    FocusAction.NEXT -> timers.next()
                    FocusAction.SKIP_BREAK -> timers.skipBreak()
                    FocusAction.PAUSE -> timers.pause()
                    FocusAction.RESUME -> timers.resume()
                    FocusAction.STOP -> timers.stop()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
