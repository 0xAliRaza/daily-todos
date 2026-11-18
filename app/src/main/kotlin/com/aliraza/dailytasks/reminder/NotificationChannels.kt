package com.aliraza.dailytasks.reminder

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
import com.aliraza.dailytasks.R

/**
 * Notification channels. A channel's sound and vibration cannot be changed by the app once created, so
 * each behaviour gets its own channel (PLAN.md section 5).
 */
object NotificationChannels {
    const val REMINDERS = "reminders"

    /** Used when the Do Not Disturb override is on: alarm audio usage, which Do Not Disturb lets through. */
    const val REMINDERS_DND = "reminders_dnd"

    /** Focus timer end alerts, one channel per signal setting. */
    const val TIMER_SOUND = "timer_sound"
    const val TIMER_VIBRATE = "timer_vibrate"
    const val TIMER_BOTH = "timer_both"

    /** The silent ongoing countdown while a round runs. */
    const val TIMER_RUNNING = "timer_running"

    private val TIMER_VIBRATION = longArrayOf(0, 600, 400, 600, 400, 600)

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
        manager.createNotificationChannels(listOf(reminders, remindersDnd) + timerChannels(context))
    }

    private fun timerChannels(context: Context): List<NotificationChannel> {
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        fun channel(id: String, name: Int, sound: Boolean, vibrate: Boolean) =
            NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_timer_description)
                setSound(if (sound) alarmSound else null, if (sound) alarmAudio else null)
                enableVibration(vibrate)
                if (vibrate) vibrationPattern = TIMER_VIBRATION
            }
        val running = NotificationChannel(
            TIMER_RUNNING,
            context.getString(R.string.channel_timer_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        return listOf(
            channel(TIMER_SOUND, R.string.channel_timer_sound, sound = true, vibrate = false),
            channel(TIMER_VIBRATE, R.string.channel_timer_vibrate, sound = false, vibrate = true),
            channel(TIMER_BOTH, R.string.channel_timer_both, sound = true, vibrate = true),
            running,
        )
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
