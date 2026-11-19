package com.aliraza.dailytasks.ui.focus

import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusRounds
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FocusDialTest {

    private val now = Instant.parse("2026-11-19T10:00:00Z")
    private val settings = Settings(focusMinutes = 25, breakMinutes = 5)
    private val delta = 1e-6f

    private fun min(value: Long): Duration = Duration.ofMinutes(value)

    @Test
    fun aRoundShorterThanAnHourFillsItsShareOfTheHourFace() {
        assertEquals(5f / 12f, DialReading(min(25), min(25)).sweep, delta)
        assertEquals(1f / 12f, DialReading(min(5), min(5)).sweep, delta)
        assertEquals(1f, DialReading(min(60), min(60)).sweep, delta)
    }

    @Test
    fun aRoundLongerThanAnHourStartsFullAndShrinksOnItsOwnScale() {
        assertEquals(1f, DialReading(min(90), min(90)).sweep, delta)
        assertEquals(0.5f, DialReading(min(45), min(90)).sweep, delta)
    }

    @Test
    fun theArcShrinksWithTheTimeLeftAndEndsEmpty() {
        assertEquals(10f / 60f, DialReading(min(10), min(25)).sweep, delta)
        assertEquals(0f, DialReading(Duration.ZERO, min(25)).sweep, delta)
    }

    @Test
    fun readyShowsTheWholeFocusRoundStillToCome() {
        val reading = dialReading(Mood.READY, FocusTimer.Idle, settings, now)
        assertEquals(DialReading(min(25), min(25)), reading)
    }

    @Test
    fun startingFocusDoesNotMoveTheArc() {
        val ready = dialReading(Mood.READY, FocusTimer.Idle, settings, now)
        val started = FocusRounds.start("task", settings, now)
        val running = dialReading(moodOf(started, "task"), started, settings, now)
        assertEquals(Mood.FOCUSING, moodOf(started, "task"))
        assertEquals(ready.sweep, running.sweep, delta)
        assertEquals(ready.left, running.left)
    }

    @Test
    fun startingTheBreakDoesNotMoveTheArc() {
        val due = FocusTimer(FocusPhase.BREAK_DUE, "task", roundLength = min(25))
        val waiting = dialReading(moodOf(due, "task"), due, settings, now)
        val onBreak = FocusRounds.startBreak(due, settings, now)
        val running = dialReading(moodOf(onBreak, "task"), onBreak, settings, now)
        assertEquals(DialReading(min(5), min(5)), waiting)
        assertEquals(waiting.sweep, running.sweep, delta)
    }

    @Test
    fun aRunningRoundCountsDownFromItsOwnLength() {
        val started = FocusRounds.start("task", settings, now)
        val later = dialReading(Mood.FOCUSING, started, settings.copy(focusMinutes = 50), now.plus(min(15)))
        assertEquals(DialReading(min(10), min(25)), later)
    }

    @Test
    fun anotherTasksTimerLeavesThisOneReady() {
        val other = FocusRounds.start("other", settings, now)
        assertEquals(Mood.READY, moodOf(other, "task"))
    }

    @Test
    fun theCaptionSaysPausedOnlyWhileARoundIsPaused() {
        val paused = FocusRounds.pause(FocusRounds.start("task", settings, now), now)
        assertEquals(DialCaption.PAUSED, dialCaption(Mood.FOCUSING, paused))
        assertEquals(DialCaption.PAUSED, dialCaption(Mood.ON_BREAK, paused))
        assertEquals(DialCaption.UNTIL, dialCaption(Mood.READY, paused))
        assertEquals(DialCaption.UNTIL, dialCaption(Mood.FOCUSING, FocusTimer.Idle))
        assertEquals(DialCaption.BREAK_UNTIL, dialCaption(Mood.BREAK_TIME, FocusTimer.Idle))
        assertEquals(DialCaption.BREAK_UNTIL, dialCaption(Mood.ON_BREAK, FocusTimer.Idle))
    }

    @Test
    fun stepsSnapToWholeMultiples() {
        assertEquals(5, stepUp(1, 5))
        assertEquals(10, stepUp(5, 5))
        assertEquals(10, stepUp(7, 5))
        assertEquals(10, stepDown(11, 5))
        assertEquals(5, stepDown(10, 5))
        assertEquals(0, stepDown(3, 5))
        assertEquals(4, stepDown(5, 1))
        assertEquals(6, stepUp(5, 1))
    }
}
