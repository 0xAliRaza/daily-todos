package com.aliraza.dailytodos.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A task as the domain sees it.
 *
 * [dueDate] and [dueTime] are wall-clock values with no time zone: 16:00 means 16:00 wherever the phone
 * is. [snoozedUntil] and [notifiedFor] are real instants, because they record moments that happened.
 */
data class Task(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.NONE,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val position: Long = 0,
    /**
     * A one-off reminder that replaces the computed one until it has been posted: a snooze, or the task's
     * own time when it was set after its heads-up had already passed.
     */
    val snoozedUntil: Instant? = null,
    /** The latest reminder instant already posted. Anything at or before it counts as handled. */
    val notifiedFor: Instant? = null,
    val completedAt: Instant? = null,
    /** Soft delete, so the action can be undone; purged later. */
    val deletedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isOpen: Boolean get() = completedAt == null && deletedAt == null
}
