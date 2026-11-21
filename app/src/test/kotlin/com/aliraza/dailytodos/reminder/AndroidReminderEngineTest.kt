package com.aliraza.dailytodos.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MainActivity
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.ClockFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The Android side of the engine, against Robolectric's alarm and notification managers. */
@RunWith(AndroidJUnit4::class)
class AndroidReminderEngineTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = app.getSystemService(AlarmManager::class.java)
    private val notificationManager = app.getSystemService(NotificationManager::class.java)
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = MutableClock(today.atTime(16, 0).atZone(zone).toInstant(), zone)

    private fun content(
        taskId: String,
        title: String,
        kind: ReminderKind = ReminderKind.DUE,
        dueDate: LocalDate = today,
    ) = ReminderContent(
        taskId = taskId,
        title = title,
        at = clock.instant(),
        kind = kind,
        dueDate = dueDate,
        dueTime = LocalTime.of(16, 0),
        listName = "Daily",
        clockFormat = ClockFormat.H12,
    )

    private fun poster() = AndroidReminderPoster(app, clock)

    private fun Notification.text(): CharSequence? = extras.getCharSequence(Notification.EXTRA_TEXT)

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
    fun postsOneUngroupedNotificationPerTaskWithCompleteAndSnooze() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val poster = poster()
        assertTrue(poster.post(content("a", "Call bank"), false))
        assertTrue(poster.post(content("b", "Gym", ReminderKind.OVERDUE), false))
        assertTrue(poster.post(content("a", "Call bank", ReminderKind.OVERDUE), false))

        assertEquals(setOf("a", "b"), poster.shownTaskIds())
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(2, notifications.size)
        notifications.forEach { notification ->
            assertNull(notification.group)
            assertEquals(listOf("Complete", "Snooze"), notification.actions.map { it.title.toString() })
            assertEquals(NotificationChannels.REMINDERS, notification.channelId)
            assertEquals(R.drawable.ic_stat_daily, notification.smallIcon.resId)
            assertEquals(app.getColor(R.color.notification_accent), notification.color)
        }
        poster.cancel("a")
        assertEquals(setOf("b"), poster.shownTaskIds())
    }

    @Test
    fun theTitleIsTheTaskAndTheBodySaysWhenAndWhere() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        poster().post(content("a", "Call bank"), false)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals("Call bank", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Today 4:00 PM · Daily", notification.text().toString())
    }

    @Test
    fun overdueIsWrittenFirstInBoldRed() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        poster().post(content("a", "Renew passport", ReminderKind.OVERDUE, dueDate = today.minusDays(1)), false)
        val text = shadowOf(notificationManager).allNotifications.single().text() as Spanned
        assertEquals("Overdue · Tue 4:00 PM · Daily", text.toString())
        val bold = text.getSpans(0, text.length, StyleSpan::class.java).single()
        assertEquals(Typeface.BOLD, bold.style)
        assertEquals(0 until "Overdue".length, text.getSpanStart(bold) until text.getSpanEnd(bold))
        val red = text.getSpans(0, text.length, ForegroundColorSpan::class.java).single()
        assertEquals(app.getColor(R.color.notification_overdue), red.foregroundColor)
        assertEquals("Overdue".length, text.getSpanEnd(red))
    }

    @Test
    fun theTestReminderIsTitledAsATestAndOpensNoTask() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        poster().post(content(TestReminder.TASK_ID, ""), false)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals("Test reminder", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        val opens = shadowOf(notification.contentIntent).savedIntent
        assertFalse(opens.hasExtra(MainActivity.EXTRA_TASK_ID))
    }

    @Test
    fun doNotDisturbOverrideUsesTheAlarmChannel() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        poster().post(content("a", "Call bank"), dndOverride = true)
        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(NotificationChannels.REMINDERS_DND, notification.channelId)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
    }

    @Test
    fun withoutNotificationPermissionNothingIsPosted() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val posted = poster().post(content("a", "Call bank"), false)
        assertFalse(posted)
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun theButtonsNameTheirTaskAndAction() {
        val intent = Intent(AndroidReminderPoster.Action.SNOOZE.name)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, "a")
        assertEquals("a" to AndroidReminderPoster.Action.SNOOZE, ReminderActionReceiver.request(intent))
    }

    /** Buttons an older version left in the shade (Done, 10 min, 1 hour) and strange intents do nothing. */
    @Test
    fun staleOrUnknownButtonsAreIgnored() {
        listOf("DONE", "SNOOZE_SHORT", "SNOOZE_LONG", "", null).forEach { name ->
            val intent = Intent(name).putExtra(ReminderActionReceiver.EXTRA_TASK_ID, "a")
            assertNull(name, ReminderActionReceiver.request(intent))
            // Returns before any work is started, so nothing can fail later either.
            ReminderActionReceiver().onReceive(app, intent)
        }
        assertNull(ReminderActionReceiver.request(Intent(AndroidReminderPoster.Action.COMPLETE.name)))
    }
}
