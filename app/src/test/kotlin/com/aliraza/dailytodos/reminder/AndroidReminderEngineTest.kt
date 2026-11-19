package com.aliraza.dailytodos.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant

/** The Android side of the engine, against Robolectric's alarm and notification managers. */
@RunWith(AndroidJUnit4::class)
class AndroidReminderEngineTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = app.getSystemService(AlarmManager::class.java)
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    @Test
    fun armsOneAlarmClockAndReplacesIt() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val armer = AndroidAlarmArmer(app)
        armer.arm(Instant.ofEpochMilli(2_000_000))
        armer.arm(Instant.ofEpochMilli(1_000_000))
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        // An alarm clock: the kind Android neither shifts nor holds back in low-power mode.
        assertEquals(1_000_000L, alarmManager.nextAlarmClock?.triggerTime)

        armer.cancel()
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun withoutExactAlarmPermissionFallsBackToAnIdleAllowedAlarm() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        AndroidAlarmArmer(app).arm(Instant.ofEpochMilli(1_000_000))
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertTrue(alarm.isAllowWhileIdle)
        assertNull(alarmManager.nextAlarmClock)
    }

    @Test
    fun postsOneUngroupedNotificationPerTaskWithThreeActions() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val poster = AndroidReminderPoster(app)
        assertTrue(poster.post(ReminderContent("a", "Call bank", Instant.EPOCH, overdue = false), false))
        assertTrue(poster.post(ReminderContent("b", "Gym", Instant.EPOCH, overdue = true), false))
        assertTrue(poster.post(ReminderContent("a", "Call bank", Instant.EPOCH, overdue = true), false))

        assertEquals(setOf("a", "b"), poster.shownTaskIds())
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(2, notifications.size)
        notifications.forEach { notification ->
            assertNull(notification.group)
            assertEquals(3, notification.actions.size)
            assertEquals(NotificationChannels.REMINDERS, notification.channelId)
        }
        poster.cancel("a")
        assertEquals(setOf("b"), poster.shownTaskIds())
    }

    @Test
    fun doNotDisturbOverrideUsesTheAlarmChannel() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AndroidReminderPoster(app).post(ReminderContent("a", "Call bank", Instant.EPOCH, false), dndOverride = true)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(NotificationChannels.REMINDERS_DND, notification.channelId)
        assertEquals(android.app.Notification.CATEGORY_ALARM, notification.category)
    }

    @Test
    fun withoutNotificationPermissionNothingIsPosted() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val posted = AndroidReminderPoster(app).post(ReminderContent("a", "Call bank", Instant.EPOCH, false), false)
        assertFalse(posted)
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }
}
