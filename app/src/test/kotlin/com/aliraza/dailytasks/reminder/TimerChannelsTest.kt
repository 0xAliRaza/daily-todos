package com.aliraza.dailytasks.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerChannelsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun theTimerEndChannelIsSilentSoTheAppAloneDecidesHowOftenItSounds() {
        NotificationChannels.ensure(context)
        val channel = manager.getNotificationChannel(NotificationChannels.TIMER_END)
        assertNotNull(channel)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
    }

    @Test
    fun earlierTimerChannelsAreRemoved() {
        listOf("timer_both", "timer_focus_both", "timer_break_sound").forEach {
            manager.createNotificationChannel(NotificationChannel(it, "old", NotificationManager.IMPORTANCE_HIGH))
        }
        NotificationChannels.ensure(context)
        assertFalse(
            manager.notificationChannels.any {
                it.id.startsWith(
                    "timer_",
                ) && it.id != NotificationChannels.TIMER_END && it.id != NotificationChannels.TIMER_RUNNING
            },
        )
    }
}
