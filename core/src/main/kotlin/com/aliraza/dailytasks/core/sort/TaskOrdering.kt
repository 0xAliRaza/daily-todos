package com.aliraza.dailytasks.core.sort

import com.aliraza.dailytasks.core.model.Task
import java.time.LocalDate
import java.time.LocalTime

/** The Today view, in display order. */
data class TodaySections(val overdue: List<Task>, val timed: List<Task>, val rest: List<Task>) {
    val all: List<Task> get() = overdue + timed + rest
}

/**
 * Sort rules (AGENTS.md, "Sorting").
 *
 * Manual order across lists follows the lists' own order, then each task's position inside its list.
 */
class TaskOrdering(private val listPositions: Map<String, Long>) {

    private val manual: Comparator<Task> = compareBy<Task>({ listPositions[it.listId] ?: Long.MAX_VALUE })
        .thenBy { it.position }
        .thenBy { it.createdAt }

    private val byTime: Comparator<Task> = compareBy<Task, LocalTime?>(nullsLast()) { it.dueTime }
        .then(manual)

    /** Overdue first (oldest first), then today's timed tasks by time, then the rest in manual order. */
    fun today(tasks: List<Task>, today: LocalDate): TodaySections {
        val dated = tasks.filter { it.isOpen }.mapNotNull { task -> task.dueDate?.let { task to it } }
        val (overdue, dueToday) = dated
            .filter { (_, date) -> !date.isAfter(today) }
            .partition { (_, date) -> date.isBefore(today) }
            .let { (late, onTime) -> late.map { it.first } to onTime.map { it.first } }
        val (timed, untimed) = dueToday.partition { it.dueTime != null }
        return TodaySections(
            overdue = overdue.sortedWith(compareBy<Task> { it.dueDate }.then(byTime)),
            timed = timed.sortedWith(byTime),
            rest = untimed.sortedWith(manual),
        )
    }

    /** One day of the Upcoming view: timed tasks by time, then the rest in manual order. */
    fun day(tasks: List<Task>): List<Task> = tasks.filter { it.isOpen }.sortedWith(byTime)

    /** A list on its own: manual order, new tasks at the bottom. */
    fun list(tasks: List<Task>): List<Task> = tasks.filter { it.isOpen }.sortedWith(manual)
}
