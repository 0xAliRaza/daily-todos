package com.aliraza.dailytodos.core.reminder

import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * When a task reminds (PLAN.md section 4, rules 2, 4 and 5).
 *
 * Reminder instants are computed, never stored. A task's [Task.notifiedFor] marks the latest instant
 * already posted; every instant at or before it counts as handled. Changing a task's date or time must
 * clear [Task.notifiedFor] and [Task.snoozedUntil] (see [rescheduled]) so the new time is eligible again.
 */
object ReminderRules {

    /** The earliest reminder of [task] not yet handled. It can lie in the past: that is a catch-up. */
    fun next(task: Task, settings: Settings, now: Instant, zone: ZoneId): Instant? =
        pending(task, settings, now, zone).minOrNull()

    /**
     * The reminder to post for [task] at [now], if any: the latest unhandled instant at or before [now].
     * Older unhandled instants are folded into it, so a task never gets two notifications at once.
     */
    fun due(task: Task, settings: Settings, now: Instant, zone: ZoneId): Instant? =
        pending(task, settings, now, zone).filter { !it.isAfter(now) }.maxOrNull()

    /**
     * The task as it should be saved after its date or time was set: earlier marks are dropped, and an
     * instant that already passed counts as handled, so typing "8am" at noon does not ping at once.
     */
    fun rescheduled(task: Task, settings: Settings, now: Instant, zone: ZoneId): Task {
        val fresh = task.copy(notifiedFor = null, snoozedUntil = null)
        return fresh.copy(notifiedFor = due(fresh, settings, now, zone))
    }

    private fun pending(task: Task, settings: Settings, now: Instant, zone: ZoneId): List<Instant> {
        if (!task.isOpen) return emptyList()
        val handled = task.notifiedFor
        val isPending = { instant: Instant -> handled == null || instant.isAfter(handled) }
        task.snoozedUntil?.takeIf(isPending)?.let { return listOf(it) }
        return candidates(task, settings, now.atZone(zone).toLocalDate())
            .map { it.atZone(zone).toInstant() }
            .filter(isPending)
    }

    /**
     * The wall-clock moments a task reminds around [today]: its own reminder while it is not yet overdue,
     * then the overdue reminder on each later morning. Today and tomorrow are enough, because the
     * scheduler runs again after every reminder it posts.
     */
    private fun candidates(task: Task, settings: Settings, today: LocalDate): List<LocalDateTime> {
        val dueDate = task.dueDate ?: return emptyList()
        return buildList {
            if (!dueDate.isBefore(today)) scheduled(task, dueDate, settings)?.let(::add)
            if (settings.overdueReminders) {
                listOf(today, today.plusDays(1))
                    .filter { it.isAfter(dueDate) }
                    .forEach { add(it.atTime(settings.overdueReminderTime)) }
            }
        }
    }

    private fun scheduled(task: Task, dueDate: LocalDate, settings: Settings): LocalDateTime? {
        val time = task.dueTime
        return when {
            time != null -> dueDate.atTime(time).minusMinutes(settings.headsUp.minutes)
            settings.dateOnlyGetsReminder -> dueDate.atTime(settings.defaultReminderTime)
            else -> null
        }
    }
}
