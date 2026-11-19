package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FocusRoundsTest {
    private val start = Instant.parse("2026-11-18T10:00:00Z")
    private val settings = Settings(focusMinutes = 25, breakMinutes = 5)

    private fun at(minutes: Long): Instant = start.plus(Duration.ofMinutes(minutes))

    private fun focusing() = FocusRounds.start("task", settings, start)

    private fun breakDue() = FocusRounds.settle(focusing(), at(25)).timer

    @Test
    fun focusBelongsToItsTaskAndRemembersItsLength() {
        val timer = focusing()
        assertEquals("task", timer.taskId)
        assertEquals(at(25), timer.endsAt)
        assertEquals(Duration.ofMinutes(25), timer.roundLength)
    }

    @Test
    fun focusEndWaitsForTheBreakToBeStarted() {
        val step = FocusRounds.settle(focusing(), at(25))
        assertEquals(FocusPhase.FOCUS, step.ended?.phase)
        assertEquals("task", step.ended?.taskId)
        assertEquals(FocusPhase.BREAK_DUE, step.timer.phase)
        assertEquals("task", step.timer.taskId)
        assertNull(step.timer.endsAt)
        assertEquals(FocusStep(step.timer), FocusRounds.settle(step.timer, at(90)))
        val onBreak = FocusRounds.startBreak(step.timer, settings, at(27))
        assertEquals(FocusPhase.BREAK, onBreak.phase)
        assertEquals(at(32), onBreak.endsAt)
        assertEquals(Duration.ofMinutes(5), onBreak.roundLength)
    }

    @Test
    fun breakEndLeavesTheTimerIdle() {
        val onBreak = FocusRounds.startBreak(breakDue(), settings, at(25))
        val step = FocusRounds.settle(onBreak, at(30))
        assertEquals(FocusPhase.BREAK, step.ended?.phase)
        assertEquals("task", step.ended?.taskId)
        assertEquals(FocusTimer.Idle, step.timer)
    }

    @Test
    fun skippingTheBreakLeavesTheTimerIdleWhetherItWaitsOrRuns() {
        assertEquals(FocusTimer.Idle, FocusRounds.skipBreak(breakDue()))
        assertEquals(FocusTimer.Idle, FocusRounds.skipBreak(FocusRounds.startBreak(breakDue(), settings, at(25))))
        val running = focusing()
        assertEquals(running, FocusRounds.skipBreak(running))
    }

    @Test
    fun startingABreakOnlyWorksWhenOneIsDue() {
        val running = focusing()
        assertEquals(running, FocusRounds.startBreak(running, settings, at(5)))
    }

    @Test
    fun nothingHappensBeforeTheEnd() {
        val timer = focusing()
        assertEquals(FocusStep(timer), FocusRounds.settle(timer, at(24)))
        assertEquals(FocusStep(FocusTimer.Idle), FocusRounds.settle(FocusTimer.Idle, at(24)))
    }

    @Test
    fun pauseKeepsTheRemainderAndResumeContinuesIt() {
        val paused = FocusRounds.pause(focusing(), at(10))
        assertEquals(Duration.ofMinutes(15), paused.pausedRemaining)
        assertEquals(FocusStep(paused), FocusRounds.settle(paused, at(60)))
        assertEquals(at(75), FocusRounds.resume(paused, at(60)).endsAt)
    }

    @Test
    fun focusedTimeLeavesPausesOut() {
        assertEquals(Duration.ofMinutes(10), FocusRounds.focused(focusing(), at(10)))
        val paused = FocusRounds.pause(focusing(), at(10))
        assertEquals(Duration.ofMinutes(10), FocusRounds.focused(paused, at(40)))
        val resumed = FocusRounds.resume(paused, at(40))
        assertEquals(Duration.ofMinutes(13), FocusRounds.focused(resumed, at(43)))
        // Never more than the round, never anything outside focus.
        assertEquals(Duration.ofMinutes(25), FocusRounds.focused(focusing(), at(90)))
        assertEquals(Duration.ZERO, FocusRounds.focused(breakDue(), at(30)))
    }

    @Test
    fun anAlertLastsWhileItsNextStepIsStillOpen() {
        val due = breakDue()
        assertEquals(true, FocusRounds.alertApplies(FocusPhase.FOCUS, "task", due))
        assertEquals(false, FocusRounds.alertApplies(FocusPhase.FOCUS, "task", FocusRounds.skipBreak(due)))
        assertEquals(
            false,
            FocusRounds.alertApplies(FocusPhase.FOCUS, "task", FocusRounds.startBreak(due, settings, at(26))),
        )
        assertEquals(false, FocusRounds.alertApplies(FocusPhase.FOCUS, "other", due))
        assertEquals(true, FocusRounds.alertApplies(FocusPhase.BREAK, "task", FocusTimer.Idle))
        assertEquals(false, FocusRounds.alertApplies(FocusPhase.BREAK, "task", focusing()))
    }

    @Test
    fun focusEndSignalsThreeTimesAndBreakEndTwice() {
        assertEquals(3, FocusRounds.pulses(FocusPhase.FOCUS))
        assertEquals(2, FocusRounds.pulses(FocusPhase.BREAK))
    }
}
