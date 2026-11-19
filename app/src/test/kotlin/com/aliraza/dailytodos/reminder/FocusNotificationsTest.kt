package com.aliraza.dailytodos.reminder

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.util.TimeZone

/** The running timer's notification writes its end time in the app's clock format, not the phone's. */
@RunWith(AndroidJUnit4::class)
class FocusNotificationsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val signals = AndroidFocusSignals(app, EndSignal(app, CoroutineScope(Dispatchers.Unconfined)))
    private val timer = FocusTimer(FocusPhase.FOCUS, "t1", endsAt = Instant.parse("2026-11-19T15:42:00Z"))
    private val zoneBefore = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() = TimeZone.setDefault(zoneBefore)

    private fun progressText(clock: ClockFormat): String {
        signals.showProgress(timer, "Write report", Settings(clockFormat = clock))
        val posted = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        return posted.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString()
    }

    @Test
    fun endsAtFollowsTheTwentyFourHourClock() = assertEquals("Focus · ends at 15:42", progressText(ClockFormat.H24))

    @Test
    fun endsAtFollowsTheTwelveHourClock() = assertEquals("Focus · ends at 3:42 PM", progressText(ClockFormat.H12))
}
