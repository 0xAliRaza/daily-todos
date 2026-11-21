package com.aliraza.dailytodos.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aliraza.dailytodos.MainActivity
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.ClockFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * What a reminder notification shows.
 *
 * @property at the instant the reminder is for (the heads-up, the due time or an overdue morning), shown
 *   as the notification's time.
 * @property dueDate the task's own due day, and [dueTime] its time, if it has one.
 * @property listName the task's list, named in the body; null leaves it out.
 */
data class ReminderContent(
    val taskId: String,
    val title: String,
    val at: Instant,
    val kind: ReminderKind,
    val dueDate: LocalDate,
    val dueTime: LocalTime?,
    val listName: String?,
    val clockFormat: ClockFormat,
) {
    /** The Reminder health test: titled by the poster, and opening the app rather than a task. */
    val isTest: Boolean get() = taskId == TestReminder.TASK_ID
}

/** Posts and removes reminder notifications: one per task, never grouped, never summarised. */
interface ReminderPoster {
    /** Returns false when the system would not show it (notifications not allowed). */
    fun post(content: ReminderContent, dndOverride: Boolean): Boolean

    fun cancel(taskId: String)

    /** Tasks that currently have a reminder in the notification shade. */
    fun shownTaskIds(): Set<String>
}

class AndroidReminderPoster(private val context: Context, private val clock: Clock) : ReminderPoster {
    private val manager = NotificationManagerCompat.from(context)

    override fun post(content: ReminderContent, dndOverride: Boolean): Boolean {
        if (!context.canPostNotifications()) return false
        NotificationChannels.ensure(context)
        val channel = if (dndOverride) NotificationChannels.REMINDERS_DND else NotificationChannels.REMINDERS
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_daily)
            .setColor(context.getColor(R.color.notification_accent))
            .setContentTitle(if (content.isTest) context.getString(R.string.reminder_test_title) else content.title)
            .setContentText(body(content))
            .setWhen(content.at.toEpochMilli())
            .setShowWhen(true)
            .setCategory(if (dndOverride) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTask(content.taskId.takeUnless { content.isTest }))
            .addAction(0, context.getString(R.string.reminder_action_complete), action(content.taskId, Action.COMPLETE))
            .addAction(0, context.getString(R.string.reminder_action_snooze), action(content.taskId, Action.SNOOZE))
            .build()
        return try {
            manager.notify(content.taskId, NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the post.
            false
        }
    }

    /** The body line; "Overdue" is bold and red, though some phones draw notification text in one colour. */
    private fun body(content: ReminderContent): CharSequence {
        val words = ReminderText.Words(
            today = context.getString(R.string.day_today),
            tomorrow = context.getString(R.string.day_tomorrow),
            overdue = context.getString(R.string.reminder_overdue),
            inMinutes = { context.getString(R.string.reminder_in_minutes, it) },
            inHours = { context.getString(R.string.reminder_in_hours, it) },
        )
        val locale = context.resources.configuration.locales[0]
        val body = ReminderText.body(content, LocalDateTime.now(clock), words, locale)
        val range = body.emphasis ?: return body.text
        return SpannableString(body.text).apply {
            val end = range.last + 1
            setSpan(StyleSpan(Typeface.BOLD), range.first, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                ForegroundColorSpan(context.getColor(R.color.notification_overdue)),
                range.first,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    override fun cancel(taskId: String) {
        manager.cancel(taskId, NOTIFICATION_ID)
    }

    override fun shownTaskIds(): Set<String> =
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.id == NOTIFICATION_ID && it.tag != null }
            .mapTo(mutableSetOf()) { it.tag }

    /** Opens [taskId]'s sheet, or just the app when there is no task. */
    private fun openTask(taskId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (taskId != null) intent.setData(taskUri(taskId, "open")).putExtra(MainActivity.EXTRA_TASK_ID, taskId)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

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

    enum class Action {
        COMPLETE,
        SNOOZE,
        ;

        companion object {
            /** The action an intent names; null for anything else, such as a button from an older version. */
            fun from(name: String?): Action? = entries.firstOrNull { it.name == name }
        }
    }

    companion object {
        /** Reminders are told apart by their tag (the task id); the id is shared. */
        const val NOTIFICATION_ID = 1
    }
}
