package com.aliraza.dailytasks.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.TodayButton
import com.aliraza.dailytasks.ui.components.rowDivider
import com.aliraza.dailytasks.ui.components.sectionDay
import com.aliraza.dailytasks.ui.components.weekdayShort
import com.aliraza.dailytasks.ui.theme.tokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

private const val DAYS_SHOWN = 7L
private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

private fun mondayOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Upcoming's strip: which week it shows and which day is picked. The add bar files new tasks under the pick. */
@Stable
class UpcomingState(private val today: LocalDate) {
    var weekStart by mutableStateOf(mondayOf(today))
        private set
    var selected by mutableStateOf(today.plusDays(1))

    val thisWeek: LocalDate get() = mondayOf(today)
    val isHome: Boolean get() = weekStart == thisWeek && selected == today.plusDays(1)

    /**
     * Moves the strip a week on or back (never before this week) and picks that week's first day still
     * ahead. Returns the picked day, or null when it couldn't move.
     */
    fun page(forward: Boolean): LocalDate? {
        if (!forward && !weekStart.isAfter(thisWeek)) return null
        weekStart = weekStart.plusWeeks(if (forward) 1 else -1)
        selected = maxOf(weekStart, today.plusDays(1))
        return selected
    }

    /** Picks [day] as the list scrolls to it, bringing its week into the strip. */
    fun follow(day: LocalDate) {
        selected = day
        weekStart = mondayOf(day)
    }

    fun home() {
        weekStart = thisWeek
        selected = today.plusDays(1)
    }

    /**
     * The days Upcoming lists, in order: the next seven days and every day of the week the strip shows,
     * each with a header even when empty so it can be jumped to and picked, and any other day with tasks.
     */
    fun days(withTasks: Set<LocalDate>): List<LocalDate> {
        val next = (1..DAYS_SHOWN).map { today.plusDays(it) }
        val shown = (0 until DAYS_SHOWN).map { weekStart.plusDays(it) }.filter { it.isAfter(today) }
        return (next + shown + withTasks.filter { it.isAfter(today) }).distinct().sorted()
    }
}

/**
 * Where a day's header sits in the Upcoming list: each earlier day takes its header plus one row per task.
 * Returns -1 for a day the list doesn't show.
 */
fun headerIndex(days: List<LocalDate>, taskCount: (LocalDate) -> Int, day: LocalDate): Int {
    val at = days.indexOf(day)
    if (at < 0) return -1
    return days.take(at).sumOf { 1 + taskCount(it) }
}

/** The day whose section holds list item [index]: the day of the nearest header at or above it. */
fun dayAt(days: List<LocalDate>, taskCount: (LocalDate) -> Int, index: Int): LocalDate? {
    var end = 0
    for (day in days) {
        end += 1 + taskCount(day)
        if (index < end) return day
    }
    return days.lastOrNull()
}

/** How far the day row must be dragged sideways to turn the week. */
private val SWIPE_DISTANCE = 48.dp

/**
 * The week over the Upcoming list: the month with week arrows (and Today to come back), then Monday to
 * Sunday with a dot under days that have tasks. Tapping a day picks it; past days are greyed and inert.
 * Swiping the days sideways turns the week, like the arrows.
 */
@Composable
fun WeekStrip(state: UpcomingState, today: LocalDate, hasTasks: (LocalDate) -> Boolean, actions: WeekStripActions) {
    Column(Modifier.rowDivider(MaterialTheme.tokens.divider, start = 0.dp).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.weekStart.format(MONTH),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (!state.isHome) TodayButton(actions.onHome)
            IconButton(onClick = { actions.onPage(false) }, enabled = state.weekStart.isAfter(state.thisWeek)) {
                Icon(AppIcons.ChevronLeft, stringResource(R.string.week_previous))
            }
            IconButton(onClick = { actions.onPage(true) }) {
                Icon(AppIcons.ChevronRight, stringResource(R.string.week_next))
            }
        }
        Row(Modifier.fillMaxWidth().weekSwipe(actions.onPage).padding(bottom = 4.dp)) {
            for (offset in 0 until DAYS_SHOWN) {
                val day = state.weekStart.plusDays(offset)
                DayCell(day, today, day == state.selected, hasTasks(day), Modifier.weight(1f)) { actions.onPick(day) }
            }
        }
    }
}

/** What the strip's controls do: back to this week, turn the week (true is forward), and pick a day. */
class WeekStripActions(val onHome: () -> Unit, val onPage: (forward: Boolean) -> Unit, val onPick: (LocalDate) -> Unit)

/** Calls [onPage] once per sideways drag that goes far enough: a drag to the left turns forward. */
@Composable
private fun Modifier.weekSwipe(onPage: (Boolean) -> Unit): Modifier {
    val page by rememberUpdatedState(onPage)
    val distance = with(LocalDensity.current) { SWIPE_DISTANCE.toPx() }
    return pointerInput(distance) {
        var dragged = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = { if (abs(dragged) >= distance) page(dragged < 0) },
        ) { change, amount ->
            change.consume()
            dragged += amount
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    picked: Boolean,
    busy: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val past = !day.isAfter(today)
    val label = sectionDay(day, today)
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !past, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                selected = picked
            }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            weekdayShort(day),
            style = MaterialTheme.typography.labelSmall,
            color = if (past) scheme.outline else scheme.onSurfaceVariant,
        )
        Box(
            Modifier.size(
                32.dp,
            ).clip(CircleShape).background(if (picked) MaterialTheme.tokens.accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (day == today || picked) FontWeight.Bold else FontWeight.Medium,
                color = dayInk(picked, past, day == today),
            )
        }
        // Invisible on days without tasks, so every cell keeps the same height.
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (busy && !past) scheme.outline else Color.Transparent))
    }
}

@Composable
private fun dayInk(picked: Boolean, past: Boolean, today: Boolean): Color = when {
    picked -> MaterialTheme.tokens.onAccent
    today -> MaterialTheme.tokens.accentText
    past -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.onBackground
}
