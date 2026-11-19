package com.aliraza.dailytodos.core

import com.aliraza.dailytodos.core.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Builds tasks for tests with only the fields that matter spelled out. */
fun task(
    id: String = "t",
    date: LocalDate? = null,
    time: LocalTime? = null,
    listId: String = "daily",
    position: Long = 0,
    notifiedFor: Instant? = null,
    snoozedUntil: Instant? = null,
    completedAt: Instant? = null,
    deletedAt: Instant? = null,
    createdAt: Instant = Instant.EPOCH,
) = Task(
    id = id,
    listId = listId,
    title = "Task $id",
    dueDate = date,
    dueTime = time,
    position = position,
    notifiedFor = notifiedFor,
    snoozedUntil = snoozedUntil,
    completedAt = completedAt,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = createdAt,
)
