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
        val posted = due.filter { entry ->
            poster.post(
                ReminderContent(entry.taskId, entry.title, Instant.ofEpochMilli(entry.atMillis), entry.overdue),
                snapshot.dndOverride,
            )
        }
        if (posted.isNotEmpty()) {
            val fired = posted.map { ScheduleMirror.Fired(it.taskId, it.atMillis, now) }
            mirror.write(snapshot.copy(firedBeforeUnlock = snapshot.firedBeforeUnlock + fired))
        }
        snapshot.upcoming.map { it.atMillis }.filter { it > now }.minOrNull()
            ?.let { alarm.arm(Instant.ofEpochMilli(it)) }
    }
}
