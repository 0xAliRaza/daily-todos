package com.aliraza.dailytasks.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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
        weekStart = mondayOf(day)
    }

    fun home() {
        weekStart = thisWeek
        selected = today.plusDays(1)
    }

    companion object {
        /** How far ahead the strip pages: ten years of weeks. */
        const val WEEKS = 520
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

/**
 * The week over the Upcoming list: the month with week arrows (and Today to come back), then Monday to
 * Sunday with a dot under days that have tasks. Tapping a day picks it; past days are greyed and inert.
 * The days are pages: they follow the finger sideways and settle on the next or previous week, and the
 * arrows (or the list scrolling into another week) turn them the same way.
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
        WeekPager(state, today, hasTasks, actions)
    }
}

/**
 * The days of each week, one page per week. Being a pager, it holds back a day's press until a touch is
 * clearly a tap, so a swipe never lights up the day it started on.
 */
@Composable
private fun WeekPager(
    state: UpcomingState,
    today: LocalDate,
    hasTasks: (LocalDate) -> Boolean,
    actions: WeekStripActions,
) {
    val pager = rememberPagerState(initialPage = state.week) { UpcomingState.WEEKS }
    val onWeek by rememberUpdatedState(actions.onWeek)
    // The arrows, Today and the list following its days change the week; the pager slides to it.
    LaunchedEffect(state.week) { if (pager.currentPage != state.week) pager.animateScrollToPage(state.week) }
    // A swipe that settles on another week shows it.
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { onWeek(it) } }
    HorizontalPager(pager, Modifier.fillMaxWidth().padding(bottom = 4.dp), key = { it }) { page ->
        val start = state.thisWeek.plusWeeks(page.toLong())
        Row(Modifier.fillMaxWidth()) {
            for (offset in 0 until DAYS_SHOWN) {
                val day = start.plusDays(offset)
                DayCell(day, today, day == state.selected, hasTasks(day), Modifier.weight(1f)) { actions.onPick(day) }
            }
        }
    }
}

/**
 * What the strip's controls do: back to this week, turn the week with the arrows (true is forward), show
 * the week a swipe settled on (counted from this week), and pick a day.
 */
class WeekStripActions(
    val onHome: () -> Unit,
    val onPage: (forward: Boolean) -> Unit,
    val onWeek: (index: Int) -> Unit,
    val onPick: (LocalDate) -> Unit,
)

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
