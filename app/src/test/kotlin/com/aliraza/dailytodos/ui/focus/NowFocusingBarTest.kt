package com.aliraza.dailytodos.ui.focus

import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The now-focusing bar's one button: Pause or Resume while focusing, Skip on any break. */
class NowFocusingBarTest {
    private val now = Instant.parse("2026-11-21T10:00:00Z")
    private val settings = Settings()

    @Test
    fun focusPausesAndAPausedRoundResumes() {
        val running = FocusRounds.start("task", settings, now)
        assertEquals(BarButton.PAUSE, barButton(running))
        assertEquals(BarButton.RESUME, barButton(FocusRounds.pause(running, now)))
    }

    @Test
    fun aBreakWaitingOrRunningIsSkipped() {
        val due = FocusTimer(FocusPhase.BREAK_DUE, "task", roundLength = Duration.ofMinutes(25))
        assertEquals(BarButton.SKIP, barButton(due))
        assertEquals(BarButton.SKIP, barButton(FocusRounds.startBreak(due, settings, now)))
    }

    @Test
    fun anIdleTimerHasNoButton() {
        assertNull(barButton(FocusTimer.Idle))
    }
}
