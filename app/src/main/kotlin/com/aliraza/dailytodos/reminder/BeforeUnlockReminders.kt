package com.aliraza.dailytodos.reminder

import java.time.Clock
import java.time.Instant

/**
 * Reminders between a restart and the first unlock, when the database cannot be read. Posts what the
 * mirror says is due, notes it for [ReminderScheduler] to record after unlock, and arms the next alarm.
 */
class BeforeUnlockReminders(
    private val mirror: ScheduleMirror,
    private val poster: ReminderPoster,
    private val alarm: AlarmArmer,
    private val clock: Clock,
) {
    fun run() {
        val snapshot = mirror.read()
        val now = clock.millis()
        val alreadyFired = snapshot.firedBeforeUnlock.map { it.taskId to it.atMillis }.toSet()
        val due = snapshot.upcoming
            .filter { it.atMillis <= now && (it.taskId to it.atMillis) !in alreadyFired }
            .groupBy { it.taskId }
            .map { (_, entries) -> entries.maxBy { it.atMillis } }
        val posted = due.filter { entry -> poster.post(contentOf(entry, snapshot, now), snapshot.dndOverride) }
        if (posted.isNotEmpty()) {
            val fired = posted.map { ScheduleMirror.Fired(it.taskId, it.atMillis, now) }
            mirror.write(snapshot.copy(firedBeforeUnlock = snapshot.firedBeforeUnlock + fired))
        }
        snapshot.upcoming.map { it.atMillis }.filter { it > now }.minOrNull()
            ?.let { alarm.arm(Instant.ofEpochMilli(it)) }
    }

    /** Words the notification as the engine would; an entry from an older file falls back to its alarm time. */
    private fun contentOf(entry: ScheduleMirror.Entry, snapshot: ScheduleMirror.Snapshot, now: Long): ReminderContent {
        val due = Instant.ofEpochMilli(entry.dueMillis ?: entry.atMillis).atZone(clock.zone).toLocalDateTime()
        val dueTime = due.toLocalTime().takeIf { entry.timed }
        val local = Instant.ofEpochMilli(now).atZone(clock.zone).toLocalDateTime()
        val kind = if (entry.overdue) ReminderKind.OVERDUE else ReminderKind.of(due.toLocalDate(), dueTime, local)
        return ReminderContent(
            taskId = entry.taskId,
            title = entry.title,
            at = Instant.ofEpochMilli(entry.atMillis),
            kind = kind,
            dueDate = due.toLocalDate(),
            dueTime = dueTime,
            listName = entry.listName,
            clockFormat = snapshot.clockFormat,
        )
    }
}
