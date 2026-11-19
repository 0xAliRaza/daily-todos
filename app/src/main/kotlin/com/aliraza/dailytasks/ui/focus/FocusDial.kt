package com.aliraza.dailytasks.ui.focus

import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
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
 * What the dial shows: the time [left] in a round of [length]. It reads like a kitchen timer: the arc is
 * the time left on an hour's face, or on the round's own length when that is longer, so 25 minutes fill
 * 5/12 of the ring and the arc shrinks from there as the round runs.
 */
internal data class DialReading(val left: Duration, val length: Duration) {
    val sweep: Float
        get() {
            val scale = maxOf(HOUR, length)
            return (left.toMillis().toFloat() / scale.toMillis()).coerceIn(0f, 1f)
        }

    private companion object {
        val HOUR: Duration = Duration.ofHours(1)
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

/** The one line under the time: when the round ends, or that it is paused. */
internal enum class DialCaption { UNTIL, BREAK_UNTIL, PAUSED }

internal fun dialCaption(mood: Mood, timer: FocusTimer): DialCaption = when {
    !mood.isSettled && timer.isPaused -> DialCaption.PAUSED
    mood.isBreak -> DialCaption.BREAK_UNTIL
    else -> DialCaption.UNTIL
}

/** One step shorter, snapped to a whole multiple of [step]: 11 → 10 → 5, rather than 11 → 6 → 1. */
internal fun stepDown(minutes: Int, step: Int): Int = ((minutes - 1) / step) * step

/** One step longer, snapped to a whole multiple of [step]: 1 → 5 → 10, rather than 1 → 6 → 11. */
internal fun stepUp(minutes: Int, step: Int): Int = (minutes / step + 1) * step

internal fun minutes(value: Int): Duration = Duration.ofMinutes(value.toLong())
