package com.aliraza.dailytodos.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aliraza.dailytodos.R

/**
 * Notification channels. A channel's sound and vibration cannot be changed by the app once created, so
 * each behaviour gets its own channel (PLAN.md section 5).
 */
object NotificationChannels {
    const val REMINDERS = "reminders"

    /** Used when the Do Not Disturb override is on: alarm audio usage, which Do Not Disturb lets through. */
    const val REMINDERS_DND = "reminders_dnd"

    /**
     * A round's end. Silent itself: the app sounds and buzzes a set number of times (see [EndSignal]),
     * which a channel can't do.
     */
    const val TIMER_END = "timer_end"

    /** The silent ongoing countdown while a round runs. */
    const val TIMER_RUNNING = "timer_running"

    /** Timer channels of earlier builds, removed on start. */
    private val RETIRED = listOf("timer_sound", "timer_vibrate", "timer_both") +
        listOf("focus", "break").flatMap { kind -> listOf("sound", "vibrate", "both").map { "timer_${kind}_$it" } }

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val reminders = NotificationChannel(
            REMINDERS,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_description)
            enableVibration(true)
        }
        val remindersDnd = NotificationChannel(
            REMINDERS_DND,
            context.getString(R.string.channel_reminders_dnd),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_dnd_description)
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        RETIRED.forEach(manager::deleteNotificationChannel)
        manager.createNotificationChannels(listOf(reminders, remindersDnd) + timerChannels(context))
    }

    private fun timerChannels(context: Context): List<NotificationChannel> {
        val end = NotificationChannel(
            TIMER_END,
            context.getString(R.string.channel_timer_end),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_timer_description)
            setSound(null, null)
            enableVibration(false)
        }
        val running = NotificationChannel(
            TIMER_RUNNING,
            context.getString(R.string.channel_timer_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        return listOf(end, running)
    }
}

/** Notifications are allowed: the runtime permission on Android 13 and up, and not switched off. */
fun Context.canPostNotifications(): Boolean {
    val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    return permitted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}
