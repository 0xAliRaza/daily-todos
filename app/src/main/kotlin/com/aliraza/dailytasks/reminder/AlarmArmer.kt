package com.aliraza.dailytasks.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aliraza.dailytasks.MainActivity
import java.time.Instant

/** Holds the app's single wake-up alarm. */
interface AlarmArmer {
    fun arm(at: Instant)

    fun cancel()
}

/**
 * Uses [AlarmManager.setAlarmClock]: the alarm Android neither shifts nor delays in low-power mode
 * (PLAN.md section 5). Only on Android 12, where exact alarms can be refused, does it fall back to the
 * inexact idle-allowed alarm; the Reminder health screen flags that state.
 */
class AndroidAlarmArmer(private val context: Context) : AlarmArmer {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun arm(at: Instant) {
        val operation = alarmIntent()
        if (canScheduleExact()) {
            val show = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation)
        }
    }

    override fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
