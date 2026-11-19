package com.aliraza.dailytodos.reminder

import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.TimerRepository
import java.time.Duration
import java.time.Instant

/** The focus timer's part of a scheduler run; returns when its running round ends, if one runs. */
fun interface TimerStep {
    suspend fun run(now: Instant, settings: Settings): Instant?
}

/** What the timer shows outside the app. [title] is the title of the timer's task. */
interface FocusSignals {
    /** The alert for a round that just ended, [ended] as it was when it ended. */
    fun alert(ended: FocusTimer, title: String, settings: Settings)

    /** Removes the last round's alert once its next step no longer applies to [timer]. */
    fun syncAlert(timer: FocusTimer)

    /** The quiet ongoing notification with the countdown; removed when nothing runs. */
    fun showProgress(timer: FocusTimer, title: String)
}

/**
 * Ends rounds, records the focus they held and keeps the timer's notifications true. A timer whose task
 * was completed or deleted, here or anywhere, stops quietly and keeps the focus spent so far.
 */
class FocusEngine(
    private val timers: TimerRepository,
    private val tasks: TaskRepository,
    private val signals: FocusSignals,
) : TimerStep {
    override suspend fun run(now: Instant, settings: Settings): Instant? {
        val before = stopIfTaskClosed(timers.current(), now)
        val step = FocusRounds.settle(before, now)
        if (step.timer != before) timers.save(step.timer)
        val ended = step.ended
        if (ended != null) {
            if (ended.phase == FocusPhase.FOCUS) {
                timers.record(ended.taskId, ended.roundLength ?: Duration.ZERO, ended.endsAt ?: now)
            }
            signals.alert(ended, title(ended.taskId), settings)
        } else {
            signals.syncAlert(step.timer)
        }
        signals.showProgress(step.timer, title(step.timer.taskId))
        return step.timer.endsAt
    }

    private suspend fun stopIfTaskClosed(timer: FocusTimer, now: Instant): FocusTimer {
        val taskId = timer.taskId
        if (!timer.isActive || (taskId != null && tasks.isOpen(taskId))) return timer
        timers.recordSpent(timer, now)
        timers.save(FocusTimer.Idle)
        return FocusTimer.Idle
    }

    private suspend fun title(taskId: String?): String = taskId?.let { tasks.task(it)?.title }.orEmpty()
}
