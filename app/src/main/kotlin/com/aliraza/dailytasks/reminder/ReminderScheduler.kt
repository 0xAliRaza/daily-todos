package com.aliraza.dailytasks.reminder

import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.reminder.ReminderPlan
import com.aliraza.dailytasks.data.SettingsRepository
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.db.FiredBy
import com.aliraza.dailytasks.data.db.ReminderLogDao
import com.aliraza.dailytasks.data.db.ReminderLogEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * The one path for everything that must happen at a time (PLAN.md section 5). Each run posts what is
 * due (catching up anything missed), removes notifications that no longer apply, arms exactly one alarm
 * for the earliest pending instant and refreshes the before-unlock mirror.
 *
 * Runs are serialised; it is safe to call [run] from receivers, the app start and every write.
 */
class ReminderScheduler(
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val log: ReminderLogDao,
    private val poster: ReminderPoster,
    private val alarm: AlarmArmer,
    private val mirror: ScheduleMirror,
    private val clock: Clock,
    /** When the running focus round ends, if one runs; it shares the single alarm. */
    private val timerEndsAt: suspend () -> Instant? = { null },
) {
    private val mutex = Mutex()

    suspend fun run(firedBy: FiredBy) = mutex.withLock {
        val now = clock.instant()
        recordFiredBeforeUnlock()
        val current = settings.current()
        val open = tasks.openDated()
        val plan = ReminderPlan.compute(open, current, now, clock.zone)
        val today = now.atZone(clock.zone).toLocalDate()

        val posted = plan.due.filter { post(it, current, today, firedBy, now) }.map { it.task.id }.toSet()
        if (posted.isNotEmpty()) log.trim(LOG_SIZE)
        removeStale(open.filter { it.notifiedFor != null || it.id in posted }.map { it.id }.toSet())

        val next = listOfNotNull(plan.nextAt, timerEndsAt()).minOrNull()
        if (next != null) alarm.arm(next) else alarm.cancel()
        mirror.write(snapshotOf(plan, current))
    }

    private suspend fun post(
        reminder: ReminderPlan.Reminder,
        settings: Settings,
        today: LocalDate,
        firedBy: FiredBy,
        now: Instant,
    ): Boolean {
        val task = reminder.task
        val overdue = task.dueDate?.isBefore(today) == true
        val content = ReminderContent(task.id, task.title, reminder.at, overdue)
        if (!poster.post(content, settings.dndOverride)) return false
        tasks.markNotified(task.id, reminder.at)
        log.insert(ReminderLogEntity(taskId = task.id, dueAt = reminder.at, firedAt = now, firedBy = firedBy))
        return true
    }

    /** A shown reminder goes away once its task is done, deleted, undated or moved to a new time. */
    private fun removeStale(stillReminding: Set<String>) {
        (poster.shownTaskIds() - stillReminding).forEach(poster::cancel)
    }

    /** Moves what was posted before unlock into the database, so it is not posted twice. */
    private suspend fun recordFiredBeforeUnlock() {
        mirror.read().firedBeforeUnlock.forEach { fired ->
            val at = Instant.ofEpochMilli(fired.atMillis)
            val task = tasks.task(fired.taskId) ?: return@forEach
            if (task.notifiedFor?.isBefore(at) != false) tasks.markNotified(task.id, at)
            log.insert(
                ReminderLogEntity(
                    taskId = task.id,
                    dueAt = at,
                    firedAt = Instant.ofEpochMilli(fired.firedAtMillis),
                    firedBy = FiredBy.BEFORE_UNLOCK,
                ),
            )
        }
    }

    private fun snapshotOf(plan: ReminderPlan, settings: Settings) = ScheduleMirror.Snapshot(
        upcoming = plan.upcoming.take(ScheduleMirror.MAX_ENTRIES).map { reminder ->
            val atDate = reminder.at.atZone(clock.zone).toLocalDate()
            ScheduleMirror.Entry(
                taskId = reminder.task.id,
                title = reminder.task.title,
                atMillis = reminder.at.toEpochMilli(),
                overdue = reminder.task.dueDate?.isBefore(atDate) == true,
            )
        },
        dndOverride = settings.dndOverride,
    )

    companion object {
        /** Reminder history keeps the newest rows only (PLAN.md section 4). */
        const val LOG_SIZE = 200
    }
}
