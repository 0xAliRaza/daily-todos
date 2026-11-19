package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.Settings
import java.time.Duration
import java.time.Instant

enum class FocusPhase {
    IDLE,
    FOCUS,

    /** Focus has ended and the break waits for a tap: "Start break", or skip it. */
    BREAK_DUE,
    BREAK,
}

/**
 * The focus timer as stored. It always belongs to one task, [taskId], while it is not [FocusPhase.IDLE].
 * A running round has [endsAt]; a paused one has [pausedRemaining] instead. [roundLength] is the length
 * the round was started with, so the time actually spent can be measured; while the break waits it is
 * still the focus round that just ended.
 */
data class FocusTimer(
    val phase: FocusPhase = FocusPhase.IDLE,
    val taskId: String? = null,
    val endsAt: Instant? = null,
    val pausedRemaining: Duration? = null,
    val roundLength: Duration? = null,
) {
    val isRunning: Boolean get() = endsAt != null
    val isPaused: Boolean get() = pausedRemaining != null

    /** A round runs or the break waits: the timer is busy with [taskId]. */
    val isActive: Boolean get() = phase != FocusPhase.IDLE

    /** Time left in the current round at [now]; zero once it has ended. */
    fun remaining(now: Instant): Duration = pausedRemaining
        ?: endsAt?.let { Duration.between(now, it).coerceAtLeast(Duration.ZERO) }
        ?: Duration.ZERO

    /**
     * How much of the round has passed at [now], from 0 to 1: the elapsed share of [roundLength]. It holds
     * still while paused, and is full while the break waits, as the focus round before it has ended.
     * Zero when idle.
     */
    fun progress(now: Instant): Float {
        val length = roundLength?.takeIf { isActive && !it.isZero } ?: return 0f
        val elapsed = length.minus(remaining(now)).toMillis().toFloat()
        return (elapsed / length.toMillis()).coerceIn(0f, 1f)
    }

    companion object {
        val Idle = FocusTimer()
    }
}

/**
 * What a scheduler run did to the timer: the new state, and the round that just ended, as it was when it
 * ended (its phase, task and length), if one did.
 */
data class FocusStep(val timer: FocusTimer, val ended: FocusTimer? = null)

/**
 * The timer's rules (PLAN.md section 6), with no clock or storage of their own. There is focus and there
 * is a break, nothing else, and nothing starts by itself: when focus ends the break waits for "Start
 * break" or a skip, and when the break ends (or is skipped) the timer is idle again, ready for the next
 * focus on the same task.
 */
object FocusRounds {
    /** Shorter partial focus is not worth a line in the stats. */
    val MIN_SESSION: Duration = Duration.ofMinutes(1)

    fun start(taskId: String, settings: Settings, now: Instant): FocusTimer {
        val length = minutes(settings.focusMinutes)
        return FocusTimer(FocusPhase.FOCUS, taskId, endsAt = now.plus(length), roundLength = length)
    }

    fun pause(timer: FocusTimer, now: Instant): FocusTimer {
        val endsAt = timer.endsAt ?: return timer
        return timer.copy(endsAt = null, pausedRemaining = Duration.between(now, endsAt).coerceAtLeast(Duration.ZERO))
    }

    fun resume(timer: FocusTimer, now: Instant): FocusTimer {
        val left = timer.pausedRemaining ?: return timer
        return timer.copy(endsAt = now.plus(left), pausedRemaining = null)
    }

    /** Starts the break that waits after a focus round. */
    fun startBreak(timer: FocusTimer, settings: Settings, now: Instant): FocusTimer {
        if (timer.phase != FocusPhase.BREAK_DUE) return timer
        val length = minutes(settings.breakMinutes)
        return timer.copy(
            phase = FocusPhase.BREAK,
            endsAt = now.plus(length),
            pausedRemaining = null,
            roundLength = length,
        )
    }

    /** Skips the break, waiting or running: the timer is idle, ready for the next focus. */
    fun skipBreak(timer: FocusTimer): FocusTimer = if (timer.phase in BREAK_PHASES) FocusTimer.Idle else timer

    private val BREAK_PHASES = setOf(FocusPhase.BREAK, FocusPhase.BREAK_DUE)

    /** How much of the running focus round has been spent by [now], pauses left out. Zero outside focus. */
    fun focused(timer: FocusTimer, now: Instant): Duration {
        val length = timer.roundLength
        if (timer.phase != FocusPhase.FOCUS || length == null) return Duration.ZERO
        return length.minus(timer.remaining(now)).coerceIn(Duration.ZERO, length)
    }

    /** Moves the timer on past a round that has ended by [now]. */
    fun settle(timer: FocusTimer, now: Instant): FocusStep {
        val endsAt = timer.endsAt
        return when {
            endsAt == null || endsAt.isAfter(now) -> FocusStep(timer)

            timer.phase == FocusPhase.FOCUS -> FocusStep(
                timer.copy(phase = FocusPhase.BREAK_DUE, endsAt = null, pausedRemaining = null),
                ended = timer,
            )

            timer.phase == FocusPhase.BREAK -> FocusStep(FocusTimer.Idle, ended = timer)

            else -> FocusStep(timer)
        }
    }

    /**
     * Whether the alert for a round that ended in [ended] on [taskId] still offers the next step for
     * [timer]. A focus end offers the break, so it stays while that break waits; a break end offers the next
     * focus, so it stays while nothing runs.
     */
    fun alertApplies(ended: FocusPhase, taskId: String?, timer: FocusTimer): Boolean = when (ended) {
        FocusPhase.FOCUS -> timer.phase == FocusPhase.BREAK_DUE && timer.taskId == taskId
        FocusPhase.BREAK -> timer.phase == FocusPhase.IDLE
        else -> false
    }

    /** How many times a round's end sounds: three for focus, two for a break. Then it stops. */
    fun pulses(ended: FocusPhase): Int = if (ended == FocusPhase.FOCUS) FOCUS_PULSES else BREAK_PULSES

    private const val FOCUS_PULSES = 3
    private const val BREAK_PULSES = 2

    private fun minutes(value: Int): Duration = Duration.ofMinutes(value.toLong())
}
