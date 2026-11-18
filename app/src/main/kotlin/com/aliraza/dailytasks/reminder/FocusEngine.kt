package com.aliraza.dailytasks.reminder

import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusRounds
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.TimerSignal
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.TimerRepository
import java.time.Instant

/** The focus timer's part of a scheduler run; returns when its running round ends, if one runs. */
fun interface TimerStep {
    suspend fun run(now: Instant, settings: Settings): Instant?
}

/** What the timer shows outside the app. */
interface FocusSignals {
    /** The alarm-like alert for a round that just ended. */
    fun alert(ended: FocusPhase, signal: TimerSignal)

    fun clearAlert()

    /** The quiet ongoing notification with the countdown; removed when nothing runs. */
    fun showProgress(timer: FocusTimer)
}

class FocusEngine(
    private val timers: TimerRepository,
    private val tasks: TaskRepository,
    private val signals: FocusSignals,
) : TimerStep {
    override suspend fun run(now: Instant, settings: Settings): Instant? {
        val before = timers.current()
        val taskOpen = before.taskId?.let { tasks.task(it)?.isOpen } ?: true
        val step = FocusRounds.settle(before, settings, now, taskOpen)
        if (step.timer != before) timers.save(step.timer)
        val ended = step.ended
        when {
            ended != null -> signals.alert(ended, settings.timerSignal)
            step.timer.phase == FocusPhase.IDLE || step.timer.phase == FocusPhase.FOCUS -> signals.clearAlert()
        }
        signals.showProgress(step.timer)
        return step.timer.endsAt
    }
}
