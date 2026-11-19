package com.aliraza.dailytodos.core.parse

import com.aliraza.dailytodos.core.model.Priority
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The words the add bar writes into the line when a date, priority or list is picked by hand. They are
 * exactly what [QuickAddParser] reads, so a picked value is highlighted and can be tapped off like a
 * typed one.
 */
object QuickAddWords {
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private const val YEAR_DAYS = 365L

    /** "today", "tomorrow", "25 dec" within the coming year, else "25 dec 2027". */
    fun date(date: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, date)
        val words = when {
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days in 2 until YEAR_DAYS -> date.format(DAY_MONTH)
            else -> date.format(DAY_MONTH_YEAR)
        }
        return words.lowercase(Locale.ENGLISH)
    }

    /** "16:30": a 24-hour clock time, which the parser never reads as anything but itself. */
    fun time(time: LocalTime): String = "%d:%02d".format(Locale.ROOT, time.hour, time.minute)

    /** "p1" to "p3"; nothing for no priority. */
    fun priority(priority: Priority): String? = priority.takeIf { it != Priority.NONE }?.let { "p${it.level}" }

    /** "#weekendtrip" for "Weekend trip": the tag form that matches the list's name; null if none can. */
    fun list(name: String): String? = TagMatchers.listKey(name).takeIf { it.isNotEmpty() }?.let { "#$it" }
}
