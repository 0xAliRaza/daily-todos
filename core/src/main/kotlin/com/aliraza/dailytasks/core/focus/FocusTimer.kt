package com.aliraza.dailytasks.core.focus

import com.aliraza.dailytasks.core.model.Settings
import java.time.Duration
import java.time.Instant

enum class FocusPhase {
    IDLE,
    FOCUS,
    BREAK,

    /** A round has ended and the next focus waits for a tap. */
    WAITING,
}

/**
 * The focus timer as stored. A running round has [endsAt]; a paused one has [pausedRemaining] instead.
 * [round] counts focus rounds since the timer was started.
 */
data class FocusTimer(
    val phase: FocusPhase = FocusPhase.IDLE,
    val endsAt: Instant? = null,
    val pausedRemaining: Duration? = null,
    val taskId: String? = null,
    val round: Int = 0,
) {
    val isRunning: Boolean get() = endsAt != null
    val isPaused: Boolean get() = pausedRemaining != null

    /** Time left in the current round at [now]; zero once it has ended. */
    fun remaining(now: Instant): Duration = pausedRemaining
        ?: endsAt?.let { Duration.between(now, it).coerceAtLeast(Duration.ZERO) }
        ?: Duration.ZERO

    companion object {
        val Idle = FocusTimer()
    }
}

/** What a scheduler run did to the timer: the new state, and the round that just ended, if one did. */
data class FocusStep(val timer: FocusTimer, val ended: FocusPhase? = null)

/**
 * The timer's rules (PLAN.md section 6), with no clock or storage of their own. When focus ends the break
 * starts by itself; when the break ends the timer waits for "start next focus". A round that ended while
 * the app was not running is counted from its planned end, so catch-up lands where it would have.
 */
object FocusRounds {
    fun start(taskId: String?, settings: Settings, now: Instant): FocusTimer = FocusTimer(
        phase = FocusPhase.FOCUS,
        endsAt = now.plus(minutes(settings.focusMinutes)),
        taskId = taskId,
        round = 1,
    )

    fun next(timer: FocusTimer, settings: Settings, now: Instant): FocusTimer = timer.copy(
        phase = FocusPhase.FOCUS,
        endsAt = now.plus(minutes(settings.focusMinutes)),
        pausedRemaining = null,
        round = timer.round + 1,
    )

    fun pause(timer: FocusTimer, now: Instant): FocusTimer {
        val endsAt = timer.endsAt ?: return timer
        return timer.copy(endsAt = null, pausedRemaining = Duration.between(now, endsAt).coerceAtLeast(Duration.ZERO))
    }

    fun resume(timer: FocusTimer, now: Instant): FocusTimer {
        val left = timer.pausedRemaining ?: return timer
        return timer.copy(endsAt = now.plus(left), pausedRemaining = null)
    }

    /** Skips the running break straight to the next focus round. */
    fun skipBreak(timer: FocusTimer, settings: Settings, now: Instant): FocusTimer =
        if (timer.phase == FocusPhase.BREAK) next(timer, settings, now) else timer

    /**
     * Moves the timer on past any round that has ended by [now]. A timer started from a task stops, with no
     * alert, once [taskOpen] says the task is done.
     */
    fun settle(timer: FocusTimer, settings: Settings, now: Instant, taskOpen: Boolean): FocusStep {
        val endsAt = timer.endsAt
        return when {
            timer.phase == FocusPhase.IDLE -> FocusStep(timer)
            timer.taskId != null && !taskOpen -> FocusStep(FocusTimer.Idle)
            endsAt == null || endsAt.isAfter(now) -> FocusStep(timer)
            timer.phase == FocusPhase.FOCUS -> endFocus(timer, endsAt, settings, now)
            else -> FocusStep(timer.waiting(), FocusPhase.BREAK)
        }
    }

    /** Focus has ended: the break starts by itself, unless breaks are off or it is already over too. */
    private fun endFocus(timer: FocusTimer, endedAt: Instant, settings: Settings, now: Instant): FocusStep {
        if (!settings.breaksEnabled) return FocusStep(timer.waiting(), FocusPhase.FOCUS)
        val breakEnds = endedAt.plus(minutes(settings.breakMinutes))
        return if (breakEnds.isAfter(now)) {
            FocusStep(timer.copy(phase = FocusPhase.BREAK, endsAt = breakEnds), FocusPhase.FOCUS)
        } else {
            FocusStep(timer.waiting(), FocusPhase.BREAK)
        }
    }

    private fun FocusTimer.waiting() = copy(phase = FocusPhase.WAITING, endsAt = null, pausedRemaining = null)

    private fun minutes(value: Int): Duration = Duration.ofMinutes(value.toLong())
}
