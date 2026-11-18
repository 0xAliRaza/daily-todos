package com.aliraza.dailytasks.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.aliraza.dailytasks.R

/**
 * Notification channels. A channel's sound and vibration cannot be changed by the app once created, so
 * each behaviour gets its own channel (PLAN.md section 5).
 */
object NotificationChannels {
    const val REMINDERS = "reminders"

    /** Used when the Do Not Disturb override is on: alarm audio usage, which Do Not Disturb lets through. */
    const val REMINDERS_DND = "reminders_dnd"

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
        manager.createNotificationChannels(listOf(reminders, remindersDnd))
    }
}
