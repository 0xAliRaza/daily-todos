package com.aliraza.dailytasks.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.Priority
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.SectionHeader
import com.aliraza.dailytasks.ui.components.TodayButton
import com.aliraza.dailytasks.ui.components.dayLabel
import com.aliraza.dailytasks.ui.components.priorityColor
import com.aliraza.dailytasks.ui.components.rowDivider
import com.aliraza.dailytasks.ui.components.timeLabel
import com.aliraza.dailytasks.ui.components.weekdayShort
import com.aliraza.dailytasks.ui.tasks.RowContext
import com.aliraza.dailytasks.ui.tasks.TaskItem
import com.aliraza.dailytasks.ui.tasks.TasksViewModel
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HOURS = 24
private const val MINUTES_PER_HOUR = 60f
private const val BLOCK_TINT = 0.16f
private const val MONTH_DOTS = 3

/** Untimed tasks shown per day above the hours; the rest are counted. */
private const val ALL_DAY_SHOWN = 2

/** Hours shown above the current time when a view opens, so "now" sits near the top. */
private const val HOURS_BEFORE_NOW = 1

/** Where a view without today scrolls to: the start of a usual day. */
private const val DAY_START_HOUR = 7

private val HourLabelWidth = 44.dp
private val BlockHeight = 26.dp

/** Which view is shown and the day it is anchored on; the picked day in the month view. */
@Stable
class CalendarState(today: LocalDate) {
    var mode by mutableStateOf(CalendarMode.WEEK)
    var anchor by mutableStateOf(today)

    /** How far the month grid is folded while its day's tasks are scrolled. */
    internal val fold = MonthFold()

    fun open(day: LocalDate, view: CalendarMode) {
        anchor = day
        mode = view
    }
}

/**
 * Dated tasks laid out in time: a day, three days or a week as an hour grid, or a month as a grid of
 * days with the picked day's tasks below. Tasks have no length, so each is a short block at its time.
 */
@Composable
fun CalendarContent(viewModel: TasksViewModel, ctx: RowContext, state: CalendarState, padding: PaddingValues) {
    val byDay by viewModel.byDay.collectAsStateWithLifecycle()
    val tasks = byDay ?: return
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        Header(state, ctx.today)
        ModeTabs(state)
        if (state.mode == CalendarMode.MONTH) {
            MonthView(state, ctx, tasks, padding.calculateBottomPadding())
        } else {
            TimeGrid(state, ctx, tasks, padding.calculateBottomPadding())
        }
    }
}

@Composable
private fun Header(state: CalendarState, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            state.anchor.format(MONTH_YEAR),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (state.anchor != today) TodayButton(onClick = { state.anchor = today })
        IconButton(onClick = { state.anchor = step(state.mode, state.anchor, forward = false) }) {
            Icon(AppIcons.ChevronLeft, stringResource(R.string.calendar_previous))
        }
        IconButton(onClick = { state.anchor = step(state.mode, state.anchor, forward = true) }) {
            Icon(AppIcons.ChevronRight, stringResource(R.string.calendar_next))
        }
    }
}

@Composable
private fun ModeTabs(state: CalendarState) {
    val labels = mapOf(
        CalendarMode.DAY to R.string.calendar_day,
        CalendarMode.THREE_DAYS to R.string.calendar_three_days,
        CalendarMode.WEEK to R.string.calendar_week,
        CalendarMode.MONTH to R.string.calendar_month,
    )
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(CircleShape)
            .background(scheme.surfaceVariant).padding(3.dp),
    ) {
        labels.forEach { (mode, label) ->
            val on = state.mode == mode
            ModeTab(stringResource(label), on) {
                // Month again while folded opens the whole month.
                if (on && mode == CalendarMode.MONTH) scope.launch { state.fold.unfold() }
                state.mode = mode
            }
        }
    }
}

@Composable
private fun RowScope.ModeTab(label: String, on: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f).clip(CircleShape).background(if (on) scheme.surface else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick).padding(vertical = 7.dp),
    )
}

// Day, 3 days, week

@Composable
private fun TimeGrid(state: CalendarState, ctx: RowContext, tasks: Map<LocalDate, List<Task>>, bottom: Dp) {
    val days = visibleDays(state.mode, state.anchor)
    val hour = if (state.mode == CalendarMode.WEEK) 48.dp else 56.dp
    val scroll = rememberScrollState()
    val hourPx = with(LocalDensity.current) { hour.toPx() }
    val opensAt = if (ctx.today in days) (ctx.now.hour - HOURS_BEFORE_NOW).coerceAtLeast(0) else DAY_START_HOUR
    LaunchedEffect(state.mode, days.first()) { scroll.scrollTo((opensAt * hourPx).toInt()) }
    DayHeaders(days, ctx.today) { state.open(it, CalendarMode.DAY) }
    AllDayRow(days, tasks, ctx)
    val divider = MaterialTheme.tokens.divider
    Row(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = bottom)
            .drawBehind { hourLines(divider, hourPx, HourLabelWidth.toPx()) },
    ) {
        HourLabels(hour)
        days.forEach { day ->
            val timed = tasks[day].orEmpty().filter { it.dueTime != null }
            DayColumn(
                day,
                timed,
                ctx,
                GridScale(hour, showTimes = state.mode != CalendarMode.WEEK),
                Modifier.weight(1f),
            )
        }
    }
}

private fun DrawScope.hourLines(color: Color, hourPx: Float, start: Float) {
    for (h in 0..HOURS) {
        val y = h * hourPx
        drawLine(color, Offset(start, y), Offset(size.width, y), strokeWidth = 1.dp.toPx() / 2)
    }
}

/** Weekday and date over each column; today's in the accent. Tapping one opens that day. */
@Composable
private fun DayHeaders(days: List<LocalDate>, today: LocalDate, onOpen: (LocalDate) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = HourLabelWidth, top = 4.dp)) {
        days.forEach { day ->
            val isToday = day == today
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onOpen(day) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(weekdayShort(day), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Box(
                    Modifier.size(
                        30.dp,
                    ).clip(CircleShape).background(if (isToday) scheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isToday) scheme.onPrimary else scheme.onBackground,
                    )
                }
            }
        }
    }
}

/** Tasks with a date and no time, as small chips above the hours. Hidden when there are none. */
@Composable
private fun AllDayRow(days: List<LocalDate>, tasks: Map<LocalDate, List<Task>>, ctx: RowContext) {
    val untimed = days.map { day -> tasks[day].orEmpty().filter { it.dueTime == null } }
    if (untimed.all { it.isEmpty() }) return
    Row(
        Modifier.fillMaxWidth().rowDivider(MaterialTheme.tokens.divider, start = 0.dp)
            .padding(start = HourLabelWidth, top = 2.dp, bottom = 6.dp),
    ) {
        untimed.forEach { list ->
            Column(Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                list.take(ALL_DAY_SHOWN).forEach { task -> Block(task, ctx, time = null) }
                if (list.size > ALL_DAY_SHOWN) {
                    Text(
                        stringResource(R.string.calendar_more, list.size - ALL_DAY_SHOWN),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HourLabels(hour: Dp) {
    Column(Modifier.width(HourLabelWidth)) {
        for (h in 0 until HOURS) {
            Box(Modifier.height(hour).fillMaxWidth()) {
                if (h > 0) {
                    Text(
                        timeLabel(LocalTime.of(h, 0)).replace(":00", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopEnd).offset(y = (-7).dp).padding(end = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * One day's timed tasks, each at its time. Tasks in the same half hour share the width. Today's column
 * has a red line at the current time.
 */
@Composable
private fun DayColumn(day: LocalDate, timed: List<Task>, ctx: RowContext, scale: GridScale, modifier: Modifier) {
    val hour = scale.hour
    val accent = MaterialTheme.tokens.accent
    val isToday = day == ctx.today
    val nowMinutes = ctx.now.hour * MINUTES_PER_HOUR + ctx.now.minute
    Box(
        modifier.height(hour * HOURS).drawBehind {
            if (isToday) {
                val y = nowMinutes / MINUTES_PER_HOUR * hour.toPx()
                drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                drawCircle(accent, radius = 4.dp.toPx(), center = Offset(0f, y))
            }
        },
    ) {
        timed.groupBy { halfHour(it.dueTime!!) }.forEach { (slot, group) ->
            Row(
                Modifier.fillMaxWidth().offset(y = hour * (slot / 2f)).padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                group.forEach { task ->
                    Block(
                        task,
                        ctx,
                        time = task.dueTime?.takeIf { scale.showTimes },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** One hour's height; [showTimes] is off in the week, where a column is too narrow for the time. */
private class GridScale(val hour: Dp, val showTimes: Boolean)

private fun halfHour(time: LocalTime): Int = time.hour * 2 + time.minute / (MINUTES_PER_HOUR / 2).toInt()

/** A task in the calendar: tinted with its priority colour, with a bar down its edge. Tap opens it. */
@Composable
private fun Block(task: Task, ctx: RowContext, time: LocalTime?, modifier: Modifier = Modifier) {
    val ink = priorityColor(task.priority).takeIf { task.priority != Priority.NONE }
        ?: MaterialTheme.tokens.accent
    Row(
        modifier.fillMaxWidth().height(
            BlockHeight,
        ).clip(RoundedCornerShape(6.dp)).background(ink.copy(alpha = BLOCK_TINT))
            .clickable { ctx.open(task.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(ink))
        Text(
            listOfNotNull(time?.let(::timeLabel), task.title).joinToString(" "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// Month

@Composable
private fun MonthView(state: CalendarState, ctx: RowContext, tasks: Map<LocalDate, List<Task>>, bottom: Dp) {
    val days = visibleDays(CalendarMode.MONTH, state.anchor)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        days.take(DAYS_IN_WEEK).forEach { day ->
            Text(
                weekdayShort(day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val fold = state.fold
    val weeks = days.chunked(DAYS_IN_WEEK)
    val rowPx = with(LocalDensity.current) { MonthRowHeight.toPx() }
    val max = (weeks.size - 1) * rowPx
    val connection = remember(fold, max) { FoldConnection(fold, max) }
    FoldedGrid(fold, max, rowPx, pickedWeek = days.indexOf(state.anchor) / DAYS_IN_WEEK) {
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().height(MonthRowHeight)) {
                week.forEach { day ->
                    MonthCell(day, state, ctx.today, tasks[day].orEmpty(), Modifier.weight(1f))
                }
            }
        }
    }
    val picked = tasks[state.anchor].orEmpty()
    LazyColumn(
        Modifier.fillMaxSize().nestedScroll(connection),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        item { SectionHeader(dayTitle(state.anchor, ctx.today)) }
        if (picked.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.calendar_nothing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
        items(picked, key = { it.id }) { task -> TaskItem(task, ctx, ctx.meta(task, showDate = false)) }
    }
}

/** "Today · Wednesday, 18 November"; yesterday and tomorrow are named too, other days get the date. */
@Composable
private fun dayTitle(day: LocalDate, today: LocalDate): String {
    val near = abs(ChronoUnit.DAYS.between(today, day)) <= 1
    return listOfNotNull(dayLabel(day, today).takeIf { near }, day.format(LONG_DAY)).joinToString(" · ")
}

/**
 * The month's weeks in a box that shrinks as [fold] folds. As it shrinks the weeks slide up just enough
 * that the picked day's week is the one left when fully folded. Swiping the grid folds it too.
 */
@Composable
private fun FoldedGrid(fold: MonthFold, max: Float, rowPx: Float, pickedWeek: Int, weeks: @Composable () -> Unit) {
    val folded = fold.folded.coerceAtMost(max)
    val progress = if (max > 0f) folded / max else 0f
    val density = LocalDensity.current
    val height = with(density) { (max + rowPx - folded).toDp() }
    val drag = rememberDraggableState { delta -> fold.take(delta, max) }
    Box(
        Modifier.fillMaxWidth().height(height).clipToBounds()
            .draggable(drag, Orientation.Vertical, onDragStopped = { fold.settle(max) })
            .padding(horizontal = 8.dp),
    ) {
        // The weeks keep their full height and are only clipped, so the picked one is there to show.
        Column(
            Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                .offset { IntOffset(0, -(progress * pickedWeek * rowPx).roundToInt()) },
        ) { weeks() }
    }
    Box(Modifier.fillMaxWidth().height(6.dp).rowDivider(MaterialTheme.tokens.divider, start = 0.dp))
}

/** Feeds the day list's scrolling to [fold] first: up folds the grid, down at the top unfolds it. */
private class FoldConnection(private val fold: MonthFold, private val max: Float) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0) Offset(0f, fold.take(available.y, max)) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y > 0) Offset(0f, fold.take(available.y, max)) else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        fold.settle(max)
        return available
    }
}

/** A day of the month: its number, filled when picked, accent when today; dots for its tasks. */
@Composable
private fun MonthCell(day: LocalDate, state: CalendarState, today: LocalDate, tasks: List<Task>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val picked = day == state.anchor
    val inMonth = day.month == state.anchor.month
    val ink = when {
        picked -> scheme.onPrimary
        day == today -> scheme.secondary
        inMonth -> scheme.onBackground
        else -> scheme.outline
    }
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).clickable { state.anchor = day }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (picked) scheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (picked || day == today) FontWeight.Bold else FontWeight.Normal,
                color = ink,
            )
        }
        Row(Modifier.height(8.dp).padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tasks.take(MONTH_DOTS).forEach { task ->
                val dot = if (task.priority == Priority.NONE) scheme.outline else priorityColor(task.priority)
                Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7

/** One week in the month grid: the day circle, its dots and a little air. */
private val MonthRowHeight = 50.dp
private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val LONG_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
