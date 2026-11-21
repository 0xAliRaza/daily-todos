package com.aliraza.dailytodos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.ui.components.DateNavLine
import com.aliraza.dailytodos.ui.components.DateStep
import com.aliraza.dailytodos.ui.components.SectionHeader
import com.aliraza.dailytodos.ui.components.priorityColor
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.sectionDate
import com.aliraza.dailytodos.ui.components.sectionDay
import com.aliraza.dailytodos.ui.components.weekdayShort
import com.aliraza.dailytodos.ui.theme.dayHeader
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.tab
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val STRIP_DAYS = 7L
private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val FREE_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d")

/** Upcoming: the week strip over the days ahead, each picked day jumped to in the list. */
@Composable
fun UpcomingContent(viewModel: TasksViewModel, ctx: RowContext, state: UpcomingState, padding: PaddingValues) {
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val todaySections by viewModel.todaySections.collectAsStateWithLifecycle()
    val byDay = upcoming ?: return
    val plan = remember(byDay, state) { state.plan(byDay) }
    val list = rememberLazyListState()
    val jump = rememberDayJump(list) { plan.indexOf(it) }
    FollowTopDay(list, jump) { plan.dayAt(it)?.let(state::follow) }
    // Today has no section here, but its strip day still shows what is due.
    val dueToday = todaySections?.let { it.timed + it.rest }.orEmpty()
    val dots = { day: LocalDate -> stripDots(if (day == ctx.today) dueToday else byDay[day].orEmpty()) }
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        val actions = WeekStripActions(
            onHome = {
                state.home()
                jump(state.selected)
            },
            onPage = { forward -> state.page(forward)?.let(jump) },
            onWeek = { index -> state.showWeek(index)?.let(jump) },
            onPick = { day ->
                state.selected = day
                jump(day)
            },
        )
        WeekStrip(state, ctx.today, dots, actions)
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
        ) {
            items(plan.lines, key = { it.key }, contentType = { it::class }) { line ->
                UpcomingLineRow(line, ctx, Modifier.animateItem()) { state.follow(it) }
            }
        }
    }
}

@Composable
private fun UpcomingLineRow(line: UpcomingLine, ctx: RowContext, modifier: Modifier, onPick: (LocalDate) -> Unit) {
    when (line) {
        is UpcomingLine.Week -> SectionHeader(
            stringResource(R.string.upcoming_this_week),
            modifier,
            count = line.count.takeIf { it > 0 },
        )

        is UpcomingLine.Day -> DayHeader(line.date, ctx.today, modifier) { onPick(line.date) }

        is UpcomingLine.Free -> FreeRun(line.first, line.last, modifier)

        is UpcomingLine.Later -> SectionHeader(stringResource(R.string.upcoming_later), modifier, count = line.count)

        is UpcomingLine.Item -> TaskItem(line.task, ctx, ctx.meta(line.task, showDate = line.later), modifier)
    }
}

/**
 * A day's header, 15 sp bold: "Mon 23 Nov", or "Tomorrow" with its date beside it in grey. Tapping it
 * picks the day for the add bar.
 */
@Composable
private fun DayHeader(day: LocalDate, today: LocalDate, modifier: Modifier, onPick: () -> Unit) {
    val tokens = MaterialTheme.tokens
    val tomorrow = day == today.plusDays(1)
    val date = sectionDate(day, today)
    Row(
        modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.upcoming_pick_day), onClick = onPick)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (tomorrow) stringResource(R.string.day_tomorrow) else date,
            style = MaterialTheme.typography.dayHeader,
            color = tokens.ink,
        )
        if (tomorrow) Text(date, style = MaterialTheme.typography.meta, color = tokens.ink2)
    }
}

/** "Tue 24 – Thu 26 · nothing planned" between two hairlines: free days take one quiet line. */
@Composable
private fun FreeRun(first: LocalDate, last: LocalDate, modifier: Modifier) {
    val tokens = MaterialTheme.tokens
    val text = if (first == last) {
        stringResource(R.string.upcoming_free_day, first.format(FREE_DAY))
    } else {
        stringResource(R.string.upcoming_free_run, first.format(FREE_DAY), last.format(FREE_DAY))
    }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(tokens.hairline))
        Text(text, style = MaterialTheme.typography.meta, color = tokens.ink2, maxLines = 1)
        Box(Modifier.weight(1f).height(1.dp).background(tokens.hairline))
    }
}

/**
 * Scrolls the list to a day's line. While it scrolls, [DayJump.running] is set so the strip doesn't
 * follow the days it passes on the way.
 */
@Stable
private class DayJump(
    private val scope: CoroutineScope,
    private val list: LazyListState,
    val index: (LocalDate) -> Int,
) : (LocalDate) -> Unit {
    var running = false
        private set

    /** Counts jumps, so a jump cut short by the next one doesn't clear [running] under it. */
    private var latest = 0

    override fun invoke(day: LocalDate) {
        val mine = ++latest
        running = true
        scope.launch {
            try {
                list.animateScrollToItem(index(day))
            } finally {
                if (latest == mine) running = false
            }
        }
    }
}

@Composable
private fun rememberDayJump(list: LazyListState, index: (LocalDate) -> Int): DayJump {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(index)
    return remember(list) { DayJump(scope, list) { latest(it) } }
}

/** As the user scrolls, hands the index of the topmost visible item to [onTop], so the strip follows. */
@Composable
private fun FollowTopDay(list: LazyListState, jump: DayJump, onTop: (Int) -> Unit) {
    val top by rememberUpdatedState(onTop)
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex }.collect { index ->
            if (list.isScrollInProgress && !jump.running) top(index)
        }
    }
}

/**
 * The week over the Upcoming list: the month with week arrows (and Today to come back), then the week's
 * days from the "Week starts on" day, with a dot per task in its priority's colour. Tapping a day picks
 * it; past days are greyed and inert, and today wears a ring.
 * The days are pages: they follow the finger sideways and settle on the next or previous week, and the
 * arrows (or the list scrolling into another week) turn them the same way.
 */
@Composable
fun WeekStrip(state: UpcomingState, today: LocalDate, dots: (LocalDate) -> List<Priority>, actions: WeekStripActions) {
    Column(Modifier.rowDivider(MaterialTheme.tokens.hairline, start = 0.dp)) {
        DateNavLine(
            state.weekStart.format(MONTH),
            onToday = actions.onHome.takeIf { !state.isHome },
            previous = DateStep(
                stringResource(R.string.week_previous),
                enabled = state.weekStart.isAfter(state.thisWeek),
            ) { actions.onPage(false) },
            next = DateStep(stringResource(R.string.week_next)) { actions.onPage(true) },
        )
        WeekPager(state, today, dots, actions, Modifier.padding(horizontal = 12.dp))
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
    dots: (LocalDate) -> List<Priority>,
    actions: WeekStripActions,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState(initialPage = state.week) { UpcomingState.WEEKS }
    val onWeek by rememberUpdatedState(actions.onWeek)
    // The arrows, Today and the list following its days change the week; the pager slides to it.
    LaunchedEffect(state.week) { if (pager.currentPage != state.week) pager.animateScrollToPage(state.week) }
    // A swipe that settles on another week shows it.
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { onWeek(it) } }
    HorizontalPager(pager, modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), key = { it }) { page ->
        val start = state.thisWeek.plusWeeks(page.toLong())
        Row(Modifier.fillMaxWidth()) {
            for (offset in 0 until STRIP_DAYS) {
                val day = start.plusDays(offset)
                DayCell(day, today, day == state.selected, dots(day), Modifier.weight(1f)) { actions.onPick(day) }
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

private val DAY_SIZE = 36.dp
private val DOT_SIZE = 6.dp

@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    picked: Boolean,
    dots: List<Priority>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    val isToday = day == today
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
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(weekdayShort(day), style = MaterialTheme.typography.tab, color = tokens.ink2)
        Box(
            Modifier.size(DAY_SIZE).clip(CircleShape)
                .background(if (picked) tokens.accent else Color.Transparent)
                .then(if (isToday && !picked) Modifier.border(2.dp, tokens.accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.taskTitle,
                fontWeight = FontWeight.Bold,
                color = dayInk(picked, past && !isToday, isToday),
            )
        }
        // Keeps its height on days without tasks, so every cell lines up.
        Row(Modifier.height(DOT_SIZE), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (!past || isToday) {
                dots.forEach { Box(Modifier.size(DOT_SIZE).clip(CircleShape).background(priorityColor(it))) }
            }
        }
    }
}

@Composable
private fun dayInk(picked: Boolean, past: Boolean, today: Boolean): Color = when {
    picked -> MaterialTheme.tokens.onAccent
    today -> MaterialTheme.tokens.accent
    past -> MaterialTheme.tokens.ink3
    else -> MaterialTheme.tokens.ink
}
