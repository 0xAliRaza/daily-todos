package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FocusProgressTest {
    private val start = Instant.parse("2026-11-18T10:00:00Z")
    private val settings = Settings(focusMinutes = 20, breakMinutes = 5)

    private fun at(minutes: Long): Instant = start.plus(Duration.ofMinutes(minutes))

    private fun focusing() = FocusRounds.start("task", settings, start)

    @Test
    fun progressIsTheElapsedShareOfTheRound() {
        val timer = focusing()
        assertEquals(0f, timer.progress(start), DELTA)
        assertEquals(0.25f, timer.progress(at(5)), DELTA)
        assertEquals(1f, timer.progress(at(20)), DELTA)
        assertEquals(1f, timer.progress(at(90)), DELTA)
    }

    @Test
    fun aPausedRoundHoldsItsProgress() {
        val paused = FocusRounds.pause(focusing(), at(10))
        assertEquals(0.5f, paused.progress(at(10)), DELTA)
        assertEquals(0.5f, paused.progress(at(60)), DELTA)
    }

    @Test
    fun aBreakCountsAgainstItsOwnLength() {
        val due = FocusRounds.settle(focusing(), at(20)).timer
        assertEquals(1f, due.progress(at(21)), DELTA)
        val onBreak = FocusRounds.startBreak(due, settings, at(22))
        assertEquals(0.2f, onBreak.progress(at(23)), DELTA)
    }

    @Test
    fun anIdleTimerHasNoProgress() {
        assertEquals(0f, FocusTimer.Idle.progress(start), DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
