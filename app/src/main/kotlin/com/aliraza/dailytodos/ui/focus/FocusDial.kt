package com.aliraza.dailytodos.ui.focus

import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import java.time.Duration
import java.time.Instant

/**
 * How the timer screen looks for its task. [READY] when nothing runs for it: after a break, after a skip,
 * or the first time. [BREAK_TIME] once focus has ended and the break waits.
 */
internal enum class Mood {
    READY,
    FOCUSING,
    BREAK_TIME,
    ON_BREAK,
    ;

    /** Nothing counts down yet, so the round's length can still be changed. */
    val isSettled: Boolean get() = this == READY || this == BREAK_TIME

    val isBreak: Boolean get() = this == BREAK_TIME || this == ON_BREAK
}

internal fun moodOf(timer: FocusTimer, taskId: String): Mood = when {
    timer.taskId != taskId -> Mood.READY
    timer.phase == FocusPhase.FOCUS -> Mood.FOCUSING
    timer.phase == FocusPhase.BREAK_DUE -> Mood.BREAK_TIME
    timer.phase == FocusPhase.BREAK -> Mood.ON_BREAK
    else -> Mood.READY
}

/**
 * What the dial shows: the time [left] in a round of [length]. The arc is the round's progress: full when
 * it starts and emptying as it runs, whatever its length, so a 25-minute and a 90-minute round draw alike.
 */
internal data class DialReading(val left: Duration, val length: Duration) {
    val sweep: Float
        get() {
            if (length.isZero || length.isNegative) return 0f
            return (left.toMillis().toFloat() / length.toMillis()).coerceIn(0f, 1f)
        }
}

/**
 * The dial for [mood] at [now]. A settled dial shows the whole round still to come, at the length that
 * starting it will use, so the arc does not jump when the round starts.
 */
internal fun dialReading(mood: Mood, timer: FocusTimer, settings: Settings, now: Instant): DialReading {
    val planned = minutes(if (mood.isBreak) settings.breakMinutes else settings.focusMinutes)
    if (mood.isSettled) return DialReading(planned, planned)
    return DialReading(timer.remaining(now), timer.roundLength ?: planned)
}

/**
 * The one line under the time: what a settled dial is about to start, when a running round ends, or that
 * it is paused.
 */
internal enum class DialCaption { FOCUS_ROUND, BREAK, ENDS, PAUSED }

internal fun dialCaption(mood: Mood, timer: FocusTimer): DialCaption = when {
    mood == Mood.READY -> DialCaption.FOCUS_ROUND
    mood == Mood.BREAK_TIME -> DialCaption.BREAK
    timer.isPaused -> DialCaption.PAUSED
    else -> DialCaption.ENDS
}

/**
 * The line under the task's title. While focusing it counts the round under way ([round], one more than
 * the rounds done) and the time focused before it; otherwise it sums the task's focus so far.
 */
internal sealed interface FocusHeadline {
    data object None : FocusHeadline

    data class Total(val focused: Duration, val rounds: Int) : FocusHeadline

    data class Round(val round: Int, val focusedBefore: Duration?) : FocusHeadline
}

internal fun focusHeadline(mood: Mood, focused: Duration, rounds: Int): FocusHeadline = when {
    mood == Mood.FOCUSING -> FocusHeadline.Round(rounds + 1, focused.takeIf { rounds > 0 })
    rounds == 0 -> FocusHeadline.None
    else -> FocusHeadline.Total(focused, rounds)
}

/** One step shorter, snapped to a whole multiple of [step]: 11 → 10 → 5, rather than 11 → 6 → 1. */
internal fun stepDown(minutes: Int, step: Int): Int = ((minutes - 1) / step) * step

/** One step longer, snapped to a whole multiple of [step]: 1 → 5 → 10, rather than 1 → 6 → 11. */
internal fun stepUp(minutes: Int, step: Int): Int = (minutes / step + 1) * step

internal fun minutes(value: Int): Duration = Duration.ofMinutes(value.toLong())
