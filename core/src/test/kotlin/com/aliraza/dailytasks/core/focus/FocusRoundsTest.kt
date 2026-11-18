package com.aliraza.dailytasks.core.focus

import com.aliraza.dailytasks.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FocusRoundsTest {
    private val start = Instant.parse("2026-11-18T10:00:00Z")
    private val settings = Settings(focusMinutes = 25, breakMinutes = 5, breaksEnabled = true)

    private fun at(minutes: Long): Instant = start.plus(Duration.ofMinutes(minutes))

    @Test
    fun focusEndStartsTheBreakByItself() {
        val timer = FocusRounds.start("t", settings, start)
        val step = FocusRounds.settle(timer, settings, at(25), taskOpen = true)
        assertEquals(FocusPhase.FOCUS, step.ended)
        assertEquals(FocusPhase.BREAK, step.timer.phase)
        assertEquals(at(30), step.timer.endsAt)
    }

    @Test
    fun breakEndWaitsForTheNextFocus() {
        val onBreak = FocusRounds.settle(FocusRounds.start(null, settings, start), settings, at(25), true).timer
        val step = FocusRounds.settle(onBreak, settings, at(30), taskOpen = true)
        assertEquals(FocusPhase.BREAK, step.ended)
        assertEquals(FocusPhase.WAITING, step.timer.phase)
        assertNull(step.timer.endsAt)
        val next = FocusRounds.next(step.timer, settings, at(31))
        assertEquals(2, next.round)
        assertEquals(at(56), next.endsAt)
    }

    @Test
    fun withBreaksOffFocusEndWaits() {
        val noBreaks = settings.copy(breaksEnabled = false)
        val step = FocusRounds.settle(FocusRounds.start(null, noBreaks, start), noBreaks, at(25), true)
        assertEquals(FocusPhase.FOCUS, step.ended)
        assertEquals(FocusPhase.WAITING, step.timer.phase)
    }

    @Test
    fun catchUpCountsTheBreakFromThePlannedEnd() {
        val timer = FocusRounds.start(null, settings, start)
        assertEquals(at(30), FocusRounds.settle(timer, settings, at(27), true).timer.endsAt)
        val late = FocusRounds.settle(timer, settings, at(40), true)
        assertEquals(FocusPhase.WAITING, late.timer.phase)
        assertEquals(FocusPhase.BREAK, late.ended)
    }

    @Test
    fun nothingHappensBeforeTheEnd() {
        val timer = FocusRounds.start(null, settings, start)
        assertEquals(FocusStep(timer), FocusRounds.settle(timer, settings, at(24), true))
    }

    @Test
    fun completingTheTaskStopsTheTimerSilently() {
        val step = FocusRounds.settle(FocusRounds.start("t", settings, start), settings, at(25), taskOpen = false)
        assertEquals(FocusStep(FocusTimer.Idle), step)
    }

    @Test
    fun pauseKeepsTheRemainderAndResumeContinuesIt() {
        val paused = FocusRounds.pause(FocusRounds.start(null, settings, start), at(10))
        assertEquals(Duration.ofMinutes(15), paused.pausedRemaining)
        assertEquals(FocusStep(paused), FocusRounds.settle(paused, settings, at(60), true))
        assertEquals(at(75), FocusRounds.resume(paused, at(60)).endsAt)
    }

    @Test
    fun skipBreakStartsTheNextFocus() {
        val onBreak = FocusRounds.settle(FocusRounds.start(null, settings, start), settings, at(25), true).timer
        val next = FocusRounds.skipBreak(onBreak, settings, at(26))
        assertEquals(FocusPhase.FOCUS, next.phase)
        assertEquals(at(51), next.endsAt)
    }
}
