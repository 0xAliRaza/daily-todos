package com.aliraza.dailytasks.data

import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusRounds
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.data.db.TimerDao
import com.aliraza.dailytasks.data.db.TimerStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The focus timer's stored state. Every change the user makes re-runs the scheduler, which owns the
 * round ends and their alerts; [save] is the scheduler's own write and does not.
 */
class TimerRepository(
    private val dao: TimerDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val onScheduleChanged: suspend () -> Unit,
) {
    fun observe(): Flow<FocusTimer> = dao.observe().map { it?.toDomain() ?: FocusTimer.Idle }

    suspend fun current(): FocusTimer = dao.get()?.toDomain() ?: FocusTimer.Idle

    suspend fun save(timer: FocusTimer) = dao.upsert(timer.toEntity())

    suspend fun start(taskId: String?) = change { _, s, now -> FocusRounds.start(taskId, s, now) }

    suspend fun next() = change { timer, s, now -> FocusRounds.next(timer, s, now) }

    suspend fun skipBreak() = change { timer, s, now -> FocusRounds.skipBreak(timer, s, now) }

    suspend fun pause() = change { timer, _, now -> FocusRounds.pause(timer, now) }

    suspend fun resume() = change { timer, _, now -> FocusRounds.resume(timer, now) }

    suspend fun stop() = change { _, _, _ -> FocusTimer.Idle }

    private suspend fun change(block: (FocusTimer, Settings, Instant) -> FocusTimer) {
        save(block(current(), settings.current(), clock.instant()))
        onScheduleChanged()
    }
}

private fun TimerStateEntity.toDomain() = FocusTimer(
    phase = FocusPhase.entries.firstOrNull { it.name == phase } ?: FocusPhase.IDLE,
    endsAt = endsAt,
    pausedRemaining = pausedRemainingMs?.let(Duration::ofMillis),
    taskId = taskId,
    round = round,
)

private fun FocusTimer.toEntity() = TimerStateEntity(
    phase = phase.name,
    endsAt = endsAt,
    taskId = taskId,
    pausedRemainingMs = pausedRemaining?.toMillis(),
    round = round,
)
