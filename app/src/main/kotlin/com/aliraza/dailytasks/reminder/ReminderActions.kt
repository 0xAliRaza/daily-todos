package com.aliraza.dailytasks.reminder

import com.aliraza.dailytasks.data.TaskRepository
import java.time.Clock
import java.time.Duration

/** What the notification buttons do. Snooze lengths are planner defaults (PLAN.md section 11). */
class ReminderActions(
    private val tasks: TaskRepository,
    private val poster: ReminderPoster,
    private val clock: Clock,
) {
    suspend fun handle(taskId: String, action: AndroidReminderPoster.Action) {
        poster.cancel(taskId)
        when (action) {
            AndroidReminderPoster.Action.DONE -> tasks.setCompleted(listOf(taskId), completed = true)
            AndroidReminderPoster.Action.SNOOZE_SHORT -> tasks.snooze(taskId, clock.instant().plus(SNOOZE_SHORT))
            AndroidReminderPoster.Action.SNOOZE_LONG -> tasks.snooze(taskId, clock.instant().plus(SNOOZE_LONG))
        }
    }

    companion object {
        val SNOOZE_SHORT: Duration = Duration.ofMinutes(10)
        val SNOOZE_LONG: Duration = Duration.ofHours(1)
    }
}
