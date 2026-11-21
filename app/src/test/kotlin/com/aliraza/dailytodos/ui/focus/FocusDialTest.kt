package com.aliraza.dailytodos.ui.focus

import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
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
    fun everyRoundStartsWithAFullArc() {
        assertEquals(1f, DialReading(min(25), min(25)).sweep, delta)
        assertEquals(1f, DialReading(min(5), min(5)).sweep, delta)
        assertEquals(1f, DialReading(min(90), min(90)).sweep, delta)
    }

    @Test
    fun aLongRoundEmptiesOnItsOwnLengthLikeAShortOne() {
        assertEquals(0.5f, DialReading(min(45), min(90)).sweep, delta)
        assertEquals(0.5f, DialReading(Duration.ofSeconds(750), min(25)).sweep, delta)
    }

    @Test
    fun theArcEmptiesWithTheTimeLeftAndEndsEmpty() {
        assertEquals(10f / 25f, DialReading(min(10), min(25)).sweep, delta)
        assertEquals(0f, DialReading(Duration.ZERO, min(25)).sweep, delta)
    }

    @Test
    fun aRoundWithNoLengthDrawsNoArc() {
        assertEquals(0f, DialReading(Duration.ZERO, Duration.ZERO).sweep, delta)
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
    fun theCaptionNamesWhatASettledDialStartsAndWhenARunningOneEnds() {
        val running = FocusRounds.start("task", settings, now)
        val paused = FocusRounds.pause(running, now)
        assertEquals(DialCaption.FOCUS_ROUND, dialCaption(Mood.READY, paused))
        assertEquals(DialCaption.BREAK, dialCaption(Mood.BREAK_TIME, FocusTimer.Idle))
        assertEquals(DialCaption.ENDS, dialCaption(Mood.FOCUSING, running))
        assertEquals(DialCaption.ENDS, dialCaption(Mood.ON_BREAK, running))
        assertEquals(DialCaption.PAUSED, dialCaption(Mood.FOCUSING, paused))
        assertEquals(DialCaption.PAUSED, dialCaption(Mood.ON_BREAK, paused))
    }

    @Test
    fun theHeadlineCountsTheRoundUnderWayWhileFocusing() {
        assertEquals(FocusHeadline.Round(1, null), focusHeadline(Mood.FOCUSING, Duration.ZERO, 0))
        assertEquals(FocusHeadline.Round(2, min(25)), focusHeadline(Mood.FOCUSING, min(25), 1))
    }

    @Test
    fun theHeadlineSumsTheTaskOtherwise() {
        assertEquals(FocusHeadline.None, focusHeadline(Mood.READY, Duration.ZERO, 0))
        assertEquals(FocusHeadline.Total(min(50), 2), focusHeadline(Mood.READY, min(50), 2))
        assertEquals(FocusHeadline.Total(min(50), 2), focusHeadline(Mood.BREAK_TIME, min(50), 2))
        assertEquals(FocusHeadline.Total(min(50), 2), focusHeadline(Mood.ON_BREAK, min(50), 2))
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
