package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The screen stays awake only with the setting on and a round counting down. */
class ScreenAwakeTest {
    private val now = Instant.parse("2026-11-19T10:00:00Z")
    private val settings = Settings()
    private val focusing = FocusRounds.start("t1", settings, now)
    private val breakDue = FocusRounds.settle(focusing, now.plusSeconds(settings.focusMinutes * 60L)).timer
    private val onBreak = FocusRounds.startBreak(breakDue, settings, now)

    @Test
    fun aRunningFocusRoundKeepsTheScreenOn() = assertTrue(keepsScreenOn(enabled = true, timer = focusing))

    @Test
    fun aRunningBreakKeepsTheScreenOn() = assertTrue(keepsScreenOn(enabled = true, timer = onBreak))

    @Test
    fun theSettingOffLetsTheScreenSleep() {
        assertFalse(keepsScreenOn(enabled = false, timer = focusing))
        assertFalse(keepsScreenOn(enabled = false, timer = onBreak))
    }

    @Test
    fun aPausedRoundLetsTheScreenSleep() {
        assertFalse(keepsScreenOn(enabled = true, timer = FocusRounds.pause(focusing, now)))
        assertFalse(keepsScreenOn(enabled = true, timer = FocusRounds.pause(onBreak, now)))
    }

    @Test
    fun aWaitingBreakLetsTheScreenSleep() = assertFalse(keepsScreenOn(enabled = true, timer = breakDue))

    @Test
    fun anIdleTimerLetsTheScreenSleep() = assertFalse(keepsScreenOn(enabled = true, timer = FocusTimer.Idle))
}
