package com.aliraza.dailytodos.reminder

import com.aliraza.dailytodos.data.TaskRepository
import java.time.Clock
import java.time.Duration

/**
 * What the notification buttons do: Complete, and Snooze for a fixed [SNOOZE]. On the test reminder,
 * Complete only dismisses it and Snooze sends it again later, so no task is ever touched.
 */
class ReminderActions(
    private val tasks: TaskRepository,
    private val poster: ReminderPoster,
    private val test: EngineTestReminder,
    private val clock: Clock,
) {
    suspend fun handle(taskId: String, action: AndroidReminderPoster.Action) {
        poster.cancel(taskId)
        val later = clock.instant().plus(SNOOZE)
        when {
            taskId == TestReminder.TASK_ID && action == AndroidReminderPoster.Action.SNOOZE -> test.schedule(later)
            taskId == TestReminder.TASK_ID -> Unit
            action == AndroidReminderPoster.Action.COMPLETE -> tasks.setCompleted(listOf(taskId), completed = true)
            action == AndroidReminderPoster.Action.SNOOZE -> tasks.snooze(taskId, later)
        }
    }

    companion object {
        val SNOOZE: Duration = Duration.ofMinutes(10)
    }
}
