package com.aliraza.dailytodos.ui.components

import androidx.annotation.StringRes
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.time.nextWeekStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** A day offered with one tap when picking a due date. */
internal data class QuickDate(@param:StringRes val label: Int, val date: LocalDate)

/** Today, tomorrow and the start of next week, for weeks that start on [weekStart]. */
internal fun quickDates(today: LocalDate, weekStart: DayOfWeek): List<QuickDate> = listOf(
    QuickDate(R.string.day_today, today),
    QuickDate(R.string.day_tomorrow, today.plusDays(1)),
    QuickDate(R.string.date_next_week, today.nextWeekStart(weekStart)),
)

/** A due date and time as picked; no date also means no time. */
data class DueChoice(val date: LocalDate?, val time: LocalTime?) {
    /** A time needs a day, so picking one on an undated choice puts it on [today]. */
    fun withTime(time: LocalTime?, today: LocalDate): DueChoice = DueChoice(date ?: today.takeIf { time != null }, time)
}
