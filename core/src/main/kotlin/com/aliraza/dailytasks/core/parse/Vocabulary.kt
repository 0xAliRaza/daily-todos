package com.aliraza.dailytasks.core.parse

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.Month

/** The words the parser knows. English only. */
internal object Vocabulary {
    val fullWeekdays: Map<String, DayOfWeek> = DayOfWeek.entries.associateBy { it.name.lowercase() }

    /** Short forms are easy to confuse with ordinary words ("sat"), so they need context to count. */
    val shortWeekdays: Map<String, DayOfWeek> = mapOf(
        "mon" to DayOfWeek.MONDAY,
        "tue" to DayOfWeek.TUESDAY,
        "tues" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY,
        "thu" to DayOfWeek.THURSDAY,
        "thur" to DayOfWeek.THURSDAY,
        "thurs" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY,
    )

    val months: Map<String, Month> = buildMap {
        Month.entries.forEach { month ->
            val name = month.name.lowercase()
            put(name, month)
            put(name.take(3), month)
        }
        put("sept", Month.SEPTEMBER)
    }

    /** Relative days that are always recognised (unless capitalised mid-sentence). */
    val relativeDays: Map<String, Long> = mapOf("today" to 0, "tomorrow" to 1, "tmrw" to 1)

    /** Short relative days; like short weekdays they need context ("Call tom" stays a name). */
    val shortRelativeDays: Map<String, Long> = mapOf("tod" to 0, "tom" to 1)

    const val TONIGHT = "tonight"
    val tonightTime: LocalTime = LocalTime.of(20, 0)

    val dayParts: Map<String, LocalTime> = mapOf(
        "morning" to LocalTime.of(9, 0),
        "afternoon" to LocalTime.of(14, 0),
        "evening" to LocalTime.of(18, 0),
    )

    val namedTimes: Map<String, LocalTime> = mapOf("noon" to LocalTime.NOON, "midnight" to LocalTime.MIDNIGHT)

    /** Words that may introduce a date and are consumed with it. */
    val datePrefixes = setOf("on", "by")

    /** Words that may introduce a weekday. "next" means strictly after today. */
    val weekdayPrefixes = setOf("on", "by", "this", "next")

    val minuteUnits = setOf("m", "min", "mins", "minute", "minutes")
    val hourUnits = setOf("h", "hr", "hrs", "hour", "hours")
    val dayUnits = setOf("d", "day", "days")
    val weekUnits = setOf("w", "wk", "wks", "week", "weeks")

    val priorities = mapOf("p1" to 1, "p2" to 2, "p3" to 3)
}
