package com.aliraza.dailytodos.data

import com.aliraza.dailytodos.core.focus.DoneDay
import com.aliraza.dailytodos.core.focus.doneByDay
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate

/** Reads what was done when: the tasks ticked off on each day, with the focus spent on each that day. */
class DoneLog(private val db: AppDatabase, private val clock: Clock) {
    /**
     * Each day from [first] to [last] with something done: the dated tasks ticked off that day, in the
     * order they were ticked, with their focus that day. Days with nothing done are missing from the map.
     */
    fun observeBetween(first: LocalDate, last: LocalDate): Flow<Map<LocalDate, DoneDay>> {
        val zone = clock.zone
        val from = first.atStartOfDay(zone).toInstant()
        val until = last.plusDays(1).atStartOfDay(zone).toInstant()
        return combine(
            db.tasks().observeCompletedBetween(from, until),
            db.focusSessions().observeEndedBetween(from, until),
        ) { rows, sessions ->
            // Undated tasks, such as a shopping list's, were never on the calendar.
            val done = rows.map { it.toDomain() }.filter { it.dueDate != null }
            doneByDay(zone, done, sessions.map { it.toDomain() })
        }
    }
}
