package com.aliraza.dailytodos.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aliraza.dailytodos.MainActivity
import com.aliraza.dailytodos.R
import java.time.Instant

/** What a reminder notification shows. */
data class ReminderContent(val taskId: String, val title: String, val dueAt: Instant, val overdue: Boolean)

/** Posts and removes reminder notifications: one per task, never grouped, never summarised. */
interface ReminderPoster {
    /** Returns false when the system would not show it (notifications not allowed). */
    fun post(content: ReminderContent, dndOverride: Boolean): Boolean

    fun cancel(taskId: String)

    /** Tasks that currently have a reminder in the notification shade. */
    fun shownTaskIds(): Set<String>
}

class AndroidReminderPoster(private val context: Context) : ReminderPoster {
    private val manager = NotificationManagerCompat.from(context)

    override fun post(content: ReminderContent, dndOverride: Boolean): Boolean {
        if (!context.canPostNotifications()) return false
        NotificationChannels.ensure(context)
        val channel = if (dndOverride) NotificationChannels.REMINDERS_DND else NotificationChannels.REMINDERS
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(if (content.overdue) context.getString(R.string.reminder_overdue) else null)
            .setWhen(content.dueAt.toEpochMilli())
            .setShowWhen(true)
            .setCategory(if (dndOverride) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTask(content.taskId))
            .addAction(0, context.getString(R.string.reminder_action_done), action(content.taskId, Action.DONE))
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_short),
                action(content.taskId, Action.SNOOZE_SHORT),
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_snooze_long),
                action(content.taskId, Action.SNOOZE_LONG),
            )
            .build()
        return try {
            manager.notify(content.taskId, NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the post.
            false
        }
    }

    override fun cancel(taskId: String) {
        manager.cancel(taskId, NOTIFICATION_ID)
    }

    override fun shownTaskIds(): Set<String> =
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.id == NOTIFICATION_ID && it.tag != null }
            .mapTo(mutableSetOf()) { it.tag }

    private fun openTask(taskId: String): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setData(taskUri(taskId, "open"))
            .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(taskId: String, action: Action): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderActionReceiver::class.java)
            .setAction(action.name)
            .setData(taskUri(taskId, action.name))
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** A distinct URI per task and action keeps their pending intents apart. */
    private fun taskUri(taskId: String, action: String): Uri =
        Uri.Builder().scheme("dailytodos").authority("task").appendPath(taskId).appendPath(action).build()

    enum class Action { DONE, SNOOZE_SHORT, SNOOZE_LONG }

    companion object {
        /** Reminders are told apart by their tag (the task id); the id is shared. */
        const val NOTIFICATION_ID = 1
    }
}
