package com.aliraza.dailytodos.reminder

import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.reminder.ReminderPlan
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.ReminderLogDao
import com.aliraza.dailytodos.data.db.ReminderLogEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The one path for everything that must happen at a time (PLAN.md section 5). Each run posts what is
 * due (catching up anything missed), removes notifications that no longer apply, arms exactly one alarm
 * for the earliest pending instant and refreshes the before-unlock mirror.
 *
 * Runs are serialised; it is safe to call [run] from receivers, the app start and every write.
 */
class ReminderScheduler(
    private val tasks: TaskRepository,
    /** Names each reminder's list in its body. */
    private val lists: ListRepository,
    private val settings: SettingsRepository,
    private val log: ReminderLogDao,
    private val poster: ReminderPoster,
    private val alarm: AlarmArmer,
    private val mirror: ScheduleMirror,
    /** The Reminder health test, which waits on the same alarm as every task. */
    private val testReminders: TestReminderStore,
    private val clock: Clock,
    /** The focus timer: ends its rounds and says when the running one ends; it shares the single alarm. */
    private val timer: TimerStep = TimerStep { _, _ -> null },
) {
    private val mutex = Mutex()

    suspend fun run(firedBy: FiredBy) = mutex.withLock {
        val now = clock.instant()
        recordFiredBeforeUnlock()
        val current = settings.current()
        val open = tasks.openDated()
        val plan = ReminderPlan.compute(open, current, now, clock.zone)
        val allLists = lists.all()
        val listNames = allLists.associate { it.id to it.name }
        val run = Run(current, listNames, allLists.firstOrNull { it.isDefault }?.name, firedBy, now)

        val posted = plan.due.filter { post(it, run) }.map { it.task.id }.toSet()
        val testPosted = postTestIfDue(run)
        if (posted.isNotEmpty() || testPosted) log.trim(LOG_SIZE)
        removeStale(open.filter { it.notifiedFor != null || it.id in posted }.map { it.id }.toSet())

        val next = listOfNotNull(plan.nextAt, timer.run(now, current), testReminders.pendingAt()).minOrNull()
        if (next != null) alarm.arm(next) else alarm.cancel()
        writeMirror(snapshotOf(plan, current, listNames))
    }

    /** What every post in one run shares. */
    private class Run(
        val settings: Settings,
        val listNames: Map<String, String>,
        val defaultListName: String?,
        val firedBy: FiredBy,
        val now: Instant,
    )

    /**
     * The mirror only matters after a restart before unlock. A failed write must not take the engine down
     * with it: the alarm is already armed, and the next run writes the mirror again.
     */
    private fun writeMirror(snapshot: ScheduleMirror.Snapshot) {
        try {
            mirror.write(snapshot)
        } catch (_: IOException) {
            // Nothing else to do; see above.
        }
    }

    private suspend fun post(reminder: ReminderPlan.Reminder, run: Run): Boolean {
        val task = reminder.task
        val dueDate = task.dueDate ?: return false
        val local = run.now.atZone(clock.zone).toLocalDateTime()
        val content = ReminderContent(
            taskId = task.id,
            title = task.title,
            at = reminder.at,
            kind = ReminderKind.of(dueDate, task.dueTime, local),
            dueDate = dueDate,
            dueTime = task.dueTime,
            listName = run.listNames[task.listId],
            clockFormat = run.settings.clockFormat,
        )
        if (!poster.post(content, run.settings.dndOverride)) return false
        tasks.markNotified(task.id, reminder.at)
        log.insert(ReminderLogEntity(taskId = task.id, dueAt = reminder.at, firedAt = run.now, firedBy = run.firedBy))
        return true
    }

    /**
     * Posts the test reminder once its minute is up, worded like a task due now in the default list. It
     * is logged under [TestReminder.TASK_ID] and touches no task. When posting is not allowed it is
     * dropped, not retried: the health screen already shows that notifications are blocked.
     */
    private suspend fun postTestIfDue(run: Run): Boolean {
        val at = testReminders.pendingAt()?.takeIf { !it.isAfter(run.now) } ?: return false
        testReminders.set(null)
        val local = at.atZone(clock.zone).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES)
        val content = ReminderContent(
            taskId = TestReminder.TASK_ID,
            title = "",
            at = at,
            kind = ReminderKind.DUE,
            dueDate = local.toLocalDate(),
            dueTime = local.toLocalTime(),
            listName = run.defaultListName,
            clockFormat = run.settings.clockFormat,
        )
        if (!poster.post(content, run.settings.dndOverride)) return false
        log.insert(
            ReminderLogEntity(taskId = TestReminder.TASK_ID, dueAt = at, firedAt = run.now, firedBy = run.firedBy),
        )
        return true
    }

    /**
     * A shown reminder goes away once its task is done, deleted, undated or moved to a new time. The test
     * reminder belongs to no task and stays until it is dismissed.
     */
    private fun removeStale(stillReminding: Set<String>) {
        (poster.shownTaskIds() - stillReminding - TestReminder.TASK_ID).forEach(poster::cancel)
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

    private fun snapshotOf(plan: ReminderPlan, settings: Settings, listNames: Map<String, String>) =
        ScheduleMirror.Snapshot(
            upcoming = plan.upcoming.take(ScheduleMirror.MAX_ENTRIES).map { entryOf(it, listNames) },
            dndOverride = settings.dndOverride,
            clockFormat = settings.clockFormat,
        )

    private fun entryOf(reminder: ReminderPlan.Reminder, listNames: Map<String, String>): ScheduleMirror.Entry {
        val task = reminder.task
        val atDate = reminder.at.atZone(clock.zone).toLocalDate()
        val due = task.dueDate?.let { date -> (task.dueTime?.let(date::atTime) ?: date.atStartOfDay()) }
        return ScheduleMirror.Entry(
            taskId = task.id,
            title = task.title,
            atMillis = reminder.at.toEpochMilli(),
            overdue = task.dueDate?.isBefore(atDate) == true,
            listName = listNames[task.listId],
            dueMillis = due?.atZone(clock.zone)?.toInstant()?.toEpochMilli(),
            timed = task.dueTime != null,
        )
    }

    companion object {
        /** Reminder history keeps the newest rows only (PLAN.md section 4). */
        const val LOG_SIZE = 200
    }
}
