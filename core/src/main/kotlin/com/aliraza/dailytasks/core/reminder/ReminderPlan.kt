package com.aliraza.dailytasks.core.reminder

import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.Task
import java.time.Instant
import java.time.ZoneId

/**
 * One scheduler pass: what to post now and when to wake up next.
 *
 * @property due tasks to notify now, each with the instant it was due (stored as `notifiedFor` once posted).
 * @property nextAt the earliest future reminder after those are posted, or null when nothing is pending.
 */
data class ReminderPlan(val due: List<DueReminder>, val nextAt: Instant?) {

    data class DueReminder(val task: Task, val dueAt: Instant)

    companion object {
        fun compute(tasks: List<Task>, settings: Settings, now: Instant, zone: ZoneId): ReminderPlan {
            val due = mutableListOf<DueReminder>()
            val upcoming = tasks.mapNotNull { task ->
                val dueAt = ReminderRules.due(task, settings, now, zone)
                val afterPosting = if (dueAt != null) {
                    due += DueReminder(task, dueAt)
                    task.copy(notifiedFor = dueAt)
                } else {
                    task
                }
                ReminderRules.next(afterPosting, settings, now, zone)
            }
            return ReminderPlan(due.sortedBy { it.dueAt }, upcoming.minOrNull())
        }
    }
}
