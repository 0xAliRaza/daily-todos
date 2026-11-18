package com.aliraza.dailytasks.core.parse

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Turns a typed time into a concrete date and time (PLAN.md section 7, rules 3 and 4). */
internal object TimeResolver {
    private const val HALF_DAY = 12
    private const val FIRST_MORNING_HOUR = 7

    fun resolve(date: LocalDate?, time: TimeValue?, now: LocalDateTime): Pair<LocalDate?, LocalTime?> = when (time) {
        null -> date to null
        is TimeValue.Exact -> (date ?: nextDateFor(time.time, now)) to time.time
        is TimeValue.Ambiguous -> ambiguous(date, time, now)
    }

    /** Rule 3: a time with no date is today if still ahead, otherwise tomorrow. */
    private fun nextDateFor(time: LocalTime, now: LocalDateTime): LocalDate =
        if (time.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)

    /** Rule 4: "at 4" is the next 4 o'clock; on another day 7 to 11 is morning, 12 noon, 1 to 6 afternoon. */
    private fun ambiguous(date: LocalDate?, time: TimeValue.Ambiguous, now: LocalDateTime): Pair<LocalDate, LocalTime> {
        val am = LocalTime.of(time.hour % HALF_DAY, time.minute)
        val pm = am.plusHours(HALF_DAY.toLong())
        val today = now.toLocalDate()
        return when (date) {
            null -> listOf(today.atTime(am), today.atTime(pm), today.plusDays(1).atTime(am))
                .first { it.isAfter(now) }
                .let { it.toLocalDate() to it.toLocalTime() }

            today -> today to (listOf(am, pm).firstOrNull { it.isAfter(now.toLocalTime()) } ?: pm)

            // 12 resolves to pm, which is noon.
            else -> date to if (time.hour in FIRST_MORNING_HOUR until HALF_DAY) am else pm
        }
    }
}
