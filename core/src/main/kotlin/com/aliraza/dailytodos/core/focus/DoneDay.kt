package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.Task
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A task ticked off on some day, [at] that wall-clock time, with the focus spent on it that day. */
data class DoneTask(val task: Task, val at: LocalTime, val focused: Duration)

/** The tasks ticked off on one day, in the order they were ticked, and their focus that day in all. */
data class DoneDay(val tasks: List<DoneTask>, val focused: Duration) {
    companion object {
        val None = DoneDay(emptyList(), Duration.ZERO)
    }
}

/**
 * What was done on [day] in [zone]: the [tasks] completed that day, each with its focus that day, in
 * completion order. A session counts on the day it ended, as in [FocusStats]; sessions on tasks not
 * done that day are left out of the total.
 */
fun doneOn(day: LocalDate, zone: ZoneId, tasks: List<Task>, sessions: List<FocusSession>): DoneDay {
    val focusByTask = sessions.filter { it.endedAt.atZone(zone).toLocalDate() == day }
        .groupBy { it.taskId }
        .mapValues { (_, own) -> own.fold(Duration.ZERO) { sum, s -> sum.plus(s.focused) } }
    val rows = tasks.mapNotNull { task ->
        val at = task.completedAt?.atZone(zone)?.takeIf { it.toLocalDate() == day } ?: return@mapNotNull null
        DoneTask(task, at.toLocalTime(), focusByTask[task.id] ?: Duration.ZERO)
    }.sortedBy { it.task.completedAt }
    return DoneDay(rows, rows.fold(Duration.ZERO) { sum, row -> sum.plus(row.focused) })
}

/** [doneOn] for every day on which one of [tasks] was completed, by day. */
fun doneByDay(zone: ZoneId, tasks: List<Task>, sessions: List<FocusSession>): Map<LocalDate, DoneDay> =
    tasks.mapNotNull { it.completedAt?.atZone(zone)?.toLocalDate() }.distinct()
        .associateWith { day -> doneOn(day, zone, tasks, sessions) }
