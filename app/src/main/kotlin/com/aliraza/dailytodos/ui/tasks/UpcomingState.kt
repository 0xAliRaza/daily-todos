package com.aliraza.dailytodos.ui.tasks

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.time.startOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How many days ahead Upcoming lists day by day; anything further sits under "Later". */
internal const val DAYS_AHEAD = 7L

/** The most dots a strip day shows. */
private const val MAX_DOTS = 3

/**
 * Upcoming's strip: which week it shows and which day is picked. The add bar files new tasks under the pick.
 * Weeks start on [firstDay], the resolved "Week starts on" setting.
 */
@Stable
class UpcomingState(private val today: LocalDate, private val firstDay: DayOfWeek) {
    var weekStart by mutableStateOf(today.startOfWeek(firstDay))
        private set
    var selected by mutableStateOf(today.plusDays(1))

    val thisWeek: LocalDate get() = today.startOfWeek(firstDay)
    val isHome: Boolean get() = weekStart == thisWeek && selected == today.plusDays(1)

    /** The week shown, counted in weeks from this one: the strip's page. */
    val week: Int get() = ChronoUnit.WEEKS.between(thisWeek, weekStart).toInt()

    /** Moves the strip a week on or back, like its arrows; see [showWeek]. */
    fun page(forward: Boolean): LocalDate? = showWeek(week + if (forward) 1 else -1)

    /**
     * Shows the week [index] weeks from this one (never before it) and picks its first day still ahead.
     * Returns the picked day, or null when that week is already shown or out of reach.
     */
    fun showWeek(index: Int): LocalDate? {
        if (index !in 0 until WEEKS || index == week) return null
        weekStart = thisWeek.plusWeeks(index.toLong())
        selected = maxOf(weekStart, today.plusDays(1))
        return selected
    }

    /** Picks [day] as the list scrolls to it, bringing its week into the strip. */
    fun follow(day: LocalDate) {
        selected = day
        weekStart = day.startOfWeek(firstDay)
    }

    fun home() {
        weekStart = thisWeek
        selected = today.plusDays(1)
    }

    /** What Upcoming lists for these open tasks, grouped by their day; see [UpcomingPlan]. */
    fun plan(byDay: Map<LocalDate, List<Task>>): UpcomingPlan = UpcomingPlan.of(today, byDay)

    companion object {
        /** How far ahead the strip pages: ten years of weeks. */
        const val WEEKS = 520
    }
}

/** One line of the Upcoming list. [key] is its stable list key. */
sealed interface UpcomingLine {
    val key: String

    /** "This week · 6", over the next seven days. */
    data class Week(val count: Int) : UpcomingLine {
        override val key: String get() = "week"
    }

    /** A day with tasks: its header, with the tasks after it. */
    data class Day(val date: LocalDate) : UpcomingLine {
        override val key: String get() = "d$date"
    }

    /** A run of days with nothing planned, folded into one quiet line ([first] may equal [last]). */
    data class Free(val first: LocalDate, val last: LocalDate) : UpcomingLine {
        override val key: String get() = "f$first"
    }

    /** "Later · 2", over every task more than a week out. */
    data class Later(val count: Int) : UpcomingLine {
        override val key: String get() = "later"
    }

    /** One task, under its day or under Later. */
    data class Item(val task: Task, val date: LocalDate, val later: Boolean) : UpcomingLine {
        override val key: String get() = task.id
    }
}

/**
 * The Upcoming list: "This week" with the next seven days, each day with tasks under its own header and
 * every run of free days folded into one line, then "Later" with everything further out, dated in its rows.
 */
class UpcomingPlan private constructor(private val today: LocalDate, val lines: List<UpcomingLine>) {

    /**
     * Where the strip's [day] sits in the list, to jump to: its header or its free run within the week;
     * further out, the first Later task on or after it. Days before tomorrow go to the top; a day past
     * every task goes to the last line.
     */
    fun indexOf(day: LocalDate): Int {
        if (!day.isAfter(today)) return 0
        val at = lines.indexOfFirst { line ->
            when (line) {
                is UpcomingLine.Day -> line.date == day
                is UpcomingLine.Free -> day in line.first..line.last
                is UpcomingLine.Item -> line.later && !line.date.isBefore(day)
                else -> false
            }
        }
        return if (at >= 0) at else lines.lastIndex
    }

    /** The day the list line at [index] belongs to, for the strip to follow as the list scrolls. */
    fun dayAt(index: Int): LocalDate? = when (val line = lines.getOrNull(index) ?: lines.lastOrNull()) {
        is UpcomingLine.Week -> today.plusDays(1)
        is UpcomingLine.Day -> line.date
        is UpcomingLine.Free -> line.first
        is UpcomingLine.Later -> today.plusDays(DAYS_AHEAD + 1)
        is UpcomingLine.Item -> line.date
        null -> null
    }

    companion object {
        fun of(today: LocalDate, byDay: Map<LocalDate, List<Task>>): UpcomingPlan {
            val week = (1..DAYS_AHEAD).map { today.plusDays(it) }
            val lines = mutableListOf<UpcomingLine>(UpcomingLine.Week(week.sumOf { byDay[it].orEmpty().size }))
            var freeFrom: LocalDate? = null
            for (day in week) {
                val tasks = byDay[day].orEmpty()
                if (tasks.isEmpty()) {
                    if (freeFrom == null) freeFrom = day
                    continue
                }
                freeFrom?.let { lines += UpcomingLine.Free(it, day.minusDays(1)) }
                freeFrom = null
                lines += UpcomingLine.Day(day)
                tasks.mapTo(lines) { UpcomingLine.Item(it, day, later = false) }
            }
            freeFrom?.let { lines += UpcomingLine.Free(it, week.last()) }
            val later = byDay.filterKeys { it.isAfter(week.last()) }.toSortedMap()
                .flatMap { (day, tasks) -> tasks.map { UpcomingLine.Item(it, day, later = true) } }
            if (later.isNotEmpty()) lines += listOf(UpcomingLine.Later(later.size)) + later
            return UpcomingPlan(today, lines)
        }
    }
}

/**
 * The dots under a strip day: one per task, the highest priorities first and tasks without one last,
 * at most [MAX_DOTS].
 */
fun stripDots(tasks: List<Task>): List<Priority> =
    tasks.map { it.priority }.sortedBy { if (it == Priority.NONE) Int.MAX_VALUE else it.level }.take(MAX_DOTS)
