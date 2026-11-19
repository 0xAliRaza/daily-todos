package com.aliraza.dailytodos.core.reminder

import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Task
import java.time.Instant
import java.time.ZoneId

/**
 * One scheduler pass: what to post now and what comes next.
 *
 * @property due tasks to notify now, each with the instant it was due (stored as `notifiedFor` once posted).
 * @property upcoming every task's next future reminder after those are posted, earliest first.
 */
data class ReminderPlan(val due: List<Reminder>, val upcoming: List<Reminder>) {

    data class Reminder(val task: Task, val at: Instant)

    /** When the alarm must next wake the app, or null when nothing is pending. */
    val nextAt: Instant? get() = upcoming.firstOrNull()?.at

    companion object {
        fun compute(tasks: List<Task>, settings: Settings, now: Instant, zone: ZoneId): ReminderPlan {
            val due = mutableListOf<Reminder>()
            val upcoming = mutableListOf<Reminder>()
            tasks.forEach { task ->
                val dueAt = ReminderRules.due(task, settings, now, zone)
                val afterPosting = if (dueAt != null) {
                    due += Reminder(task, dueAt)
                    task.copy(notifiedFor = dueAt)
                } else {
                    task
                }
                ReminderRules.next(afterPosting, settings, now, zone)?.let { upcoming += Reminder(afterPosting, it) }
            }
            return ReminderPlan(due.sortedBy { it.at }, upcoming.sortedBy { it.at })
        }
    }
}
