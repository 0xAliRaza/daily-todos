package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.time.startOfWeek
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One stretch of focus on a task, as recorded when it ended. [taskId] is null once the task was deleted
 * for good; its time still counts in the totals.
 */
data class FocusSession(val taskId: String?, val startedAt: Instant, val endedAt: Instant, val focused: Duration)

/** Focus time on one day. */
data class DayFocus(val day: LocalDate, val focused: Duration)

/** Focus time on one task; [taskId] and [title] are null for the tasks that were deleted, together. */
data class TaskFocus(val taskId: String?, val title: String?, val focused: Duration)

/** What the Focus stats screen shows. Plain totals: no points, no streaks. */
data class FocusStats(
    val total: Duration,
    val today: Duration,
    val sessions: Int,
    /** The last [DAYS] days, oldest first, ending today. */
    val lastDays: List<DayFocus>,
    /** The tasks with the most focus, at most [TOP] of them. */
    val topTasks: List<TaskFocus>,
) {
    /**
     * Focus from the start of this week to today, for weeks that start on [firstDay]. A week never reaches
     * further back than [lastDays], so they hold every day it needs.
     */
    fun thisWeek(firstDay: DayOfWeek): Duration {
        val start = lastDays.last().day.startOfWeek(firstDay)
        return lastDays.filter { !it.day.isBefore(start) }.fold(Duration.ZERO) { sum, d -> sum.plus(d.focused) }
    }

    companion object {
        const val DAYS = 7
        const val TOP = 5

        /**
         * Totals from every recorded session. A session counts on the day it ended.
         * [titles] holds the tasks that still exist; sessions of any other task count as deleted.
         */
        fun from(
            sessions: List<FocusSession>,
            titles: Map<String, String>,
            today: LocalDate,
            zone: ZoneId,
        ): FocusStats {
            val byDay = sessions.groupBy { it.endedAt.atZone(zone).toLocalDate() }.mapValues { (_, day) -> day.sum() }
            val lastDays = (DAYS - 1 downTo 0).map { back ->
                val day = today.minusDays(back.toLong())
                DayFocus(day, byDay[day] ?: Duration.ZERO)
            }
            return FocusStats(
                total = sessions.sum(),
                today = byDay[today] ?: Duration.ZERO,
                sessions = sessions.size,
                lastDays = lastDays,
                topTasks = sessions
                    .groupBy { session -> session.taskId?.takeIf { it in titles } }
                    .map { (taskId, own) -> TaskFocus(taskId, taskId?.let(titles::get), own.sum()) }
                    .sortedByDescending { it.focused }
                    .take(TOP),
            )
        }

        private fun List<FocusSession>.sum(): Duration = fold(Duration.ZERO) { sum, s -> sum.plus(s.focused) }
    }
}
