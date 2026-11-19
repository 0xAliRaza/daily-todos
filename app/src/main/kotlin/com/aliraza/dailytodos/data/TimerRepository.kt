package com.aliraza.dailytodos.data

import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusSession
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.db.FocusSessionDao
import com.aliraza.dailytodos.data.db.TimerDao
import com.aliraza.dailytodos.data.db.TimerStateEntity
import com.aliraza.dailytodos.data.db.toDomain
import com.aliraza.dailytodos.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** A task's focus so far. */
data class TaskFocusTotal(val focused: Duration, val sessions: Int) {
    companion object {
        val None = TaskFocusTotal(Duration.ZERO, 0)
    }
}

/**
 * The focus timer's stored state and the focus it has recorded. Every change the user makes re-runs the
 * scheduler, which owns the round ends and their alerts; [save] is the scheduler's own write and does not.
 */
class TimerRepository(
    private val dao: TimerDao,
    private val sessions: FocusSessionDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val onScheduleChanged: suspend () -> Unit,
) {
    fun observe(): Flow<FocusTimer> = dao.observe().map { it?.toDomain() ?: FocusTimer.Idle }

    suspend fun current(): FocusTimer = dao.get()?.toDomain() ?: FocusTimer.Idle

    suspend fun save(timer: FocusTimer) = dao.upsert(timer.toEntity())

    /** Starts focus on [taskId]. Focus running on another task ends first and keeps its time. */
    suspend fun start(taskId: String) = change { timer, s, now ->
        recordSpent(timer, now)
        FocusRounds.start(taskId, s, now)
    }

    suspend fun startBreak() = change { timer, s, now -> FocusRounds.startBreak(timer, s, now) }

    suspend fun skipBreak() = change { timer, _, _ -> FocusRounds.skipBreak(timer) }

    suspend fun pause() = change { timer, _, now -> FocusRounds.pause(timer, now) }

    suspend fun resume() = change { timer, _, now -> FocusRounds.resume(timer, now) }

    /** Ends the timer. Focus stopped early keeps the time spent so far. */
    suspend fun stop() = change { timer, _, now ->
        recordSpent(timer, now)
        FocusTimer.Idle
    }

    /** Records the focus a round held when it ended at [endedAt]; too short a stretch is not kept. */
    suspend fun record(taskId: String?, focused: Duration, endedAt: Instant) {
        if (focused < FocusRounds.MIN_SESSION) return
        sessions.insert(FocusSession(taskId, endedAt.minus(focused), endedAt, focused).toEntity())
    }

    /** Records what a focus round has spent by [now], for when it ends early. */
    suspend fun recordSpent(timer: FocusTimer, now: Instant) {
        if (timer.phase == FocusPhase.FOCUS) record(timer.taskId, FocusRounds.focused(timer, now), now)
    }

    fun observeTaskFocus(taskId: String): Flow<TaskFocusTotal> =
        sessions.observeForTask(taskId).map { TaskFocusTotal(Duration.ofMillis(it.focusedMs), it.sessions) }

    fun observeSessions(): Flow<List<FocusSession>> = sessions.observeAll().map { rows -> rows.map { it.toDomain() } }

    private suspend fun change(block: suspend (FocusTimer, Settings, Instant) -> FocusTimer) {
        save(block(current(), settings.current(), clock.instant()))
        onScheduleChanged()
    }
}

private fun TimerStateEntity.toDomain() = FocusTimer(
    phase = FocusPhase.entries.firstOrNull { it.name == phase } ?: FocusPhase.IDLE,
    taskId = taskId,
    endsAt = endsAt,
    pausedRemaining = pausedRemainingMs?.let(Duration::ofMillis),
    roundLength = roundMs?.let(Duration::ofMillis),
)

private fun FocusTimer.toEntity() = TimerStateEntity(
    phase = phase.name,
    endsAt = endsAt,
    taskId = taskId,
    pausedRemainingMs = pausedRemaining?.toMillis(),
    roundMs = roundLength?.toMillis(),
)
