package com.aliraza.dailytodos.ui.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.DoneDay
import com.aliraza.dailytodos.core.focus.DoneTask
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.ui.AddButtonClearance
import com.aliraza.dailytodos.ui.components.DateNavLine
import com.aliraza.dailytodos.ui.components.DateStep
import com.aliraza.dailytodos.ui.components.DateTone
import com.aliraza.dailytodos.ui.components.LocalClockFormat
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.components.MonthGridSize
import com.aliraza.dailytodos.ui.components.MonthWeeks
import com.aliraza.dailytodos.ui.components.PillChoice
import com.aliraza.dailytodos.ui.components.RoundCheck
import com.aliraza.dailytodos.ui.components.WeekdayLetters
import com.aliraza.dailytodos.ui.components.dayLabel
import com.aliraza.dailytodos.ui.components.dueTone
import com.aliraza.dailytodos.ui.components.hourLabel
import com.aliraza.dailytodos.ui.components.priorityColor
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.sectionDate
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.components.weekdayShort
import com.aliraza.dailytodos.ui.focus.focusDuration
import com.aliraza.dailytodos.ui.tasks.RowContext
import com.aliraza.dailytodos.ui.tasks.TaskItem
import com.aliraza.dailytodos.ui.tasks.TasksViewModel
import com.aliraza.dailytodos.ui.theme.dayHeader
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HOURS = 24
private const val SLOTS = HOURS * 2
private const val MINUTES_PER_HOUR = 60f
private const val MONTH_DOTS = 3
private const val DAYS_IN_WEEK = 7

/** Hours shown above the current time when a view opens, so "now" sits near the top. */
private const val HOURS_BEFORE_NOW = 1

/** Where a view without today scrolls to: the start of a usual day. */
private const val DAY_START_HOUR = 7

/** Date-only tasks a day shows in the No time row before the rest fold into "+N". */
private const val UNTIMED_SHOWN = 2

/** The most lines a block's label takes. */
private const val BLOCK_LINES = 2

/** A time keeps its "PM" with it when a label wraps. */
private const val NO_BREAK_SPACE = ' '

private val HourLabelWidth = 44.dp
private val BlockLine = 14.dp
private val BlockPadding = 6.dp
private val BlockMax = BlockLine * BLOCK_LINES + BlockPadding
private val CompactHeight = 14.dp
private val UntimedHeight = 28.dp
private val EdgeWidth = 3.dp
private val BlockStart = 7.dp
private val BlockEnd = 4.dp
private val BlockRadius = 6.dp

/** From this width a block cuts a long label short instead of becoming a compact pill. */
private val RoomyWidth = 96.dp

/** One week in the month grid: the day circle, its dots and a little air. */
private val MonthSize = MonthGridSize(row = 48.dp, circle = 32.dp)

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
 * Dated tasks laid out in time: a day, three days or a week as an hour grid with date-only tasks in a
 * "No time" row above it, or a month as a grid of days with the picked day's tasks below. Tasks have no
 * length, so each is a short block at its time.
 */
@Composable
fun CalendarContent(viewModel: TasksViewModel, ctx: RowContext, state: CalendarState, padding: PaddingValues) {
    val byDay by viewModel.byDay.collectAsStateWithLifecycle()
    val tasks = byDay ?: return
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        Header(state)
        ModeTabs(state)
        if (state.mode == CalendarMode.MONTH) {
            MonthView(viewModel, state, ctx, tasks, padding.calculateBottomPadding())
        } else {
            TimeGrid(state, ctx, tasks, padding.calculateBottomPadding())
        }
    }
}

/** The month under the page title, with the arrows that step the view. */
@Composable
private fun Header(state: CalendarState) {
    DateNavLine(
        state.anchor.format(MONTH_YEAR),
        onToday = null,
        previous = DateStep(stringResource(R.string.calendar_previous)) {
            state.anchor = step(state.mode, state.anchor, forward = false)
        },
        next = DateStep(stringResource(R.string.calendar_next)) {
            state.anchor = step(state.mode, state.anchor, forward = true)
        },
    )
}

@Composable
private fun ModeTabs(state: CalendarState) {
    val scope = rememberCoroutineScope()
    PillChoice(
        options = CalendarMode.entries,
        selected = state.mode,
        label = { stringResource(modeLabel(it)) },
        onPick = { mode ->
            // Month again while folded opens the whole month.
            if (mode == state.mode && mode == CalendarMode.MONTH) scope.launch { state.fold.unfold() }
            state.mode = mode
        },
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

private fun modeLabel(mode: CalendarMode): Int = when (mode) {
    CalendarMode.DAY -> R.string.calendar_day
    CalendarMode.THREE_DAYS -> R.string.calendar_three_days
    CalendarMode.WEEK -> R.string.calendar_week
    CalendarMode.MONTH -> R.string.calendar_month
}

// Day, 3 days, week

@Composable
private fun TimeGrid(state: CalendarState, ctx: RowContext, tasks: Map<LocalDate, List<Task>>, bottom: Dp) {
    val days = visibleDays(state.mode, state.anchor, LocalWeekStart.current)
    val split = days.map { splitByTime(tasks[it].orEmpty()) }
    val hour = if (state.mode == CalendarMode.WEEK) 48.dp else 56.dp
    val scroll = rememberScrollState()
    val hourPx = with(LocalDensity.current) { hour.toPx() }
    val opensAt = if (ctx.today in days) (ctx.now.hour - HOURS_BEFORE_NOW).coerceAtLeast(0) else DAY_START_HOUR
    LaunchedEffect(state.mode, days.first()) { scroll.scrollTo((opensAt * hourPx).toInt()) }
    val hints = rememberHints(split, scroll, hourPx, bottom)
    val scope = rememberCoroutineScope()
    DayHeaders(days, ctx.today, hints, onOpen = { state.open(it, CalendarMode.DAY) }) { y ->
        scope.launch { scroll.animateScrollTo((y - hourPx).toInt().coerceAtLeast(0)) }
    }
    if (split.any { it.untimed.isNotEmpty() }) NoTimeRow(days, split, ctx)
    val divider = MaterialTheme.tokens.hairline
    Row(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = bottom)
            .drawBehind { hourLines(divider, hourPx) },
    ) {
        HourLabels(hour)
        days.forEachIndexed { i, day ->
            DayColumn(
                day,
                split[i].timed,
                ctx,
                GridScale(hour, showTimes = state.mode != CalendarMode.WEEK),
                Modifier.weight(1f),
            )
        }
    }
}

/** The half hour a time falls in, 0 to 47: blocks sit at the start of theirs. */
private fun halfHour(time: LocalTime): Int = time.hour * 2 + time.minute / (MINUTES_PER_HOUR / 2).toInt()

/** For each day, its timed tasks scrolled out of view above and below, as the grid scrolls. */
@Composable
private fun rememberHints(split: List<DaySplit>, scroll: ScrollState, hourPx: Float, bottom: Dp): List<Hidden> {
    val density = LocalDensity.current
    // Only the tab bar hides rows; the add button's clearance below it is still in view.
    val bottomPx = with(density) { (bottom - AddButtonClearance).coerceAtLeast(0.dp).toPx() }
    val blockPx = with(density) { (BlockLine + BlockPadding).toPx() }
    val tops = remember(split, hourPx) {
        split.map { day -> day.timed.mapNotNull { task -> task.dueTime?.let { halfHour(it) * hourPx / 2 } } }
    }
    val hints by remember(tops, scroll, blockPx, bottomPx) {
        derivedStateOf {
            val top = scroll.value.toFloat()
            tops.map { hidden(it, blockPx, top, top + scroll.viewportSize - bottomPx) }
        }
    }
    return hints
}

private fun DrawScope.hourLines(color: Color, hourPx: Float) {
    for (h in 0..HOURS) {
        val y = h * hourPx
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
}

/**
 * Weekday and date over each column, today's on the accent; tapping one opens that day. Under each,
 * "↑ 2" when that day has tasks scrolled out of view above, or "↓ 1" below; tapping it scrolls to them.
 */
@Composable
private fun DayHeaders(
    days: List<LocalDate>,
    today: LocalDate,
    hints: List<Hidden>,
    onOpen: (LocalDate) -> Unit,
    onHint: (Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = HourLabelWidth, end = 4.dp, top = 8.dp)) {
        days.forEachIndexed { i, day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                DayHeader(day, today) { onOpen(day) }
                HintLine(hints[i], onHint)
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, today: LocalDate, onOpen: () -> Unit) {
    val tokens = MaterialTheme.tokens
    val isToday = day == today
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(weekdayShort(day), style = MaterialTheme.typography.labelSmall, color = tokens.ink2)
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (isToday) tokens.accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.dayHeader,
                color = when {
                    isToday -> tokens.onAccent
                    day.isBefore(today) -> tokens.ink2
                    else -> tokens.ink
                },
            )
        }
    }
}

/** "↑1", "↓2" or both; empty space of the same height when the day's tasks are all in view. */
@Composable
private fun HintLine(hint: Hidden, onHint: (Float) -> Unit) {
    val target = hint.above.maxOrNull() ?: hint.below.minOrNull()
    val text = listOfNotNull(
        stringResource(R.string.calendar_hidden_short_above, hint.above.size).takeIf { hint.above.isNotEmpty() },
        stringResource(R.string.calendar_hidden_short_below, hint.below.size).takeIf { hint.below.isNotEmpty() },
    ).joinToString(" ")
    val spoken = listOfNotNull(
        pluralStringResource(R.plurals.calendar_hidden_above, hint.above.size, hint.above.size)
            .takeIf { hint.above.isNotEmpty() },
        pluralStringResource(R.plurals.calendar_hidden_below, hint.below.size, hint.below.size)
            .takeIf { hint.below.isNotEmpty() },
    ).joinToString(", ")
    Box(
        Modifier.height(HintHeight).fillMaxWidth().then(
            if (target == null) {
                Modifier
            } else {
                Modifier.clickable(role = Role.Button) { onHint(target) }.semantics { contentDescription = spoken }
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = gutterStyle().copy(fontWeight = FontWeight.Bold), color = MaterialTheme.tokens.ink2)
    }
}

private val HintHeight = 16.dp

/** The hour labels and the No time label: 11 sp in ink-2. */
@Composable
private fun gutterStyle(): TextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp)

/**
 * Date-only tasks, one column per day, between the day headers and the hour grid. A day shows two;
 * "+N" opens the row to show every one.
 */
@Composable
private fun NoTimeRow(days: List<LocalDate>, split: List<DaySplit>, ctx: RowContext) {
    var all by remember(days.first(), days.size) { mutableStateOf(false) }
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().rowDivider(tokens.hairline, start = 0.dp)
            .padding(top = 4.dp, end = 4.dp, bottom = 8.dp),
    ) {
        Text(
            stringResource(R.string.task_no_time),
            style = gutterStyle(),
            color = tokens.ink2,
            modifier = Modifier.width(HourLabelWidth).padding(start = 6.dp, top = 4.dp, end = 2.dp),
        )
        days.indices.forEach { i ->
            val untimed = split[i].untimed
            val shown = if (all) untimed else untimed.take(UNTIMED_SHOWN)
            Column(Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                shown.forEach { task -> UntimedBlock(task, ctx) }
                if (shown.size < untimed.size) MoreBlock(untimed.size - shown.size) { all = true }
            }
        }
    }
}

/** A date-only task: as many of its title's whole words as fit on one line. */
@Composable
private fun UntimedBlock(task: Task, ctx: RowContext) {
    BoxWithConstraints {
        val wrapped = rememberWrap(task.title, maxWidth, lines = 1)
        TaskBlock(task, ctx, UntimedHeight, labelled = wrapped.lines.isNotEmpty()) {
            BlockText(wrapped.lines.firstOrNull().orEmpty(), task, ctx, lines = 1)
        }
    }
}

@Composable
private fun MoreBlock(count: Int, onClick: () -> Unit) {
    val tokens = MaterialTheme.tokens
    Box(
        Modifier.fillMaxWidth().height(UntimedHeight).clip(RoundedCornerShape(BlockRadius))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.calendar_more, count), style = blockStyle(), color = tokens.ink2)
    }
}

@Composable
private fun HourLabels(hour: Dp) {
    Column(Modifier.width(HourLabelWidth)) {
        for (h in 0 until HOURS) {
            Box(Modifier.height(hour).fillMaxWidth()) {
                if (h > 0) {
                    Text(
                        hourLabel(h, LocalClockFormat.current),
                        style = gutterStyle().copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.tokens.ink2,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * One day's timed tasks, each at the start of its half hour. Tasks in the same half hour share the
 * width, and each block is as tall as the gap to the next one allows. Today's column has a line in the
 * accent at the current time.
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
                drawCircle(accent, radius = 5.dp.toPx(), center = Offset(0f, y))
            }
        },
    ) {
        val slots = timed.groupBy { task -> task.dueTime?.let(::halfHour) ?: 0 }
        val starts = slots.keys.sorted()
        starts.forEachIndexed { i, slot ->
            val room = minOf(hour / 2 * ((starts.getOrNull(i + 1) ?: SLOTS) - slot) - 1.dp, BlockMax)
            Row(
                Modifier.fillMaxWidth().offset(y = hour * (slot / 2f)).padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                slots.getValue(slot).forEach { task ->
                    TimedBlock(task, ctx, room, scale.showTimes, Modifier.weight(1f))
                }
            }
        }
    }
}

/** One hour's height; [showTimes] is off in the week, where a column is too narrow for the time. */
private class GridScale(val hour: Dp, val showTimes: Boolean)

/**
 * A task at its time: its label in whole words when they fit in [room], else cut short on a wide
 * block, else a compact pill that still shows the priority edge and opens the task.
 */
@Composable
private fun TimedBlock(task: Task, ctx: RowContext, room: Dp, showTime: Boolean, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val time = task.dueTime?.takeIf { showTime }?.let { timeLabel(it).replace(' ', NO_BREAK_SPACE) }
        val text = listOfNotNull(time, task.title).joinToString(" ")
        val lines = with(LocalDensity.current) {
            linesThatFit(room.toPx(), BlockPadding.toPx(), BlockLine.toPx(), BLOCK_LINES)
        }
        val wrapped = rememberWrap(text, maxWidth, lines)
        when (blockFit(wrapped, lines, roomy = maxWidth >= RoomyWidth)) {
            BlockFit.WHOLE -> TaskBlock(task, ctx, BlockLine * wrapped.lines.size + BlockPadding) {
                BlockText(wrapped.lines.joinToString("\n"), task, ctx, wrapped.lines.size)
            }

            BlockFit.CLIPPED -> TaskBlock(task, ctx, BlockLine * lines + BlockPadding) {
                BlockText(text, task, ctx, lines)
            }

            BlockFit.COMPACT -> TaskBlock(task, ctx, CompactHeight, labelled = false) {}
        }
    }
}

/** [text] wrapped in whole words to fit a block [width] wide, measured in the block's own type. */
@Composable
private fun rememberWrap(text: String, width: Dp, lines: Int): Wrapped {
    val measurer = rememberTextMeasurer()
    val style = blockStyle()
    val room = with(LocalDensity.current) { (width - BlockStart - BlockEnd).toPx() }
    return remember(text, room, lines, style) {
        wrapWhole(text, room, lines) { part ->
            measurer.measure(part, style, softWrap = false, maxLines = 1).size.width.toFloat()
        }
    }
}

/**
 * A task on the grid: a neutral fill with its priority colour down the left edge (ink-3 without one).
 * Tapping opens it. A block without a [labelled] title says the title to a screen reader instead.
 */
@Composable
private fun TaskBlock(
    task: Task,
    ctx: RowContext,
    height: Dp,
    labelled: Boolean = true,
    label: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    val edge = priorityColor(task.priority)
    Box(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(BlockRadius)).background(tokens.fill)
            .drawBehind { drawRect(edge, size = Size(EdgeWidth.toPx(), size.height)) }
            .clickable { ctx.open(task.id) }
            .then(if (labelled) Modifier else Modifier.semantics { contentDescription = task.title })
            .padding(start = BlockStart, end = BlockEnd),
        contentAlignment = Alignment.CenterStart,
    ) { label() }
}

/** A block's label: ink, or the overdue red for a task from a day before today. */
@Composable
private fun BlockText(text: String, task: Task, ctx: RowContext, lines: Int) {
    val tokens = MaterialTheme.tokens
    Text(
        text,
        style = blockStyle(),
        color = if (task.dueTone(ctx.now) == DateTone.OVERDUE) tokens.danger else tokens.ink,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Block labels: 12 sp semi-bold on 14 sp lines. */
@Composable
private fun blockStyle(): TextStyle = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp, letterSpacing = 0.sp)

// Month

@Composable
private fun MonthView(
    viewModel: TasksViewModel,
    state: CalendarState,
    ctx: RowContext,
    tasks: Map<LocalDate, List<Task>>,
    bottom: Dp,
) {
    val days = visibleDays(CalendarMode.MONTH, state.anchor, LocalWeekStart.current)
    val done by remember(days.first(), days.last()) { viewModel.doneBetween(days.first(), days.last()) }
        .collectAsStateWithLifecycle(emptyMap())
    WeekdayLetters(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
    val fold = state.fold
    val rowPx = with(LocalDensity.current) { MonthSize.row.toPx() }
    val max = (days.size / DAYS_IN_WEEK - 1) * rowPx
    val connection = remember(fold, max) { FoldConnection(fold, max) }
    // Open tasks only: what is done leaves no dot.
    val dotColors = Priority.entries.associateWith { priorityColor(it) }
    FoldedGrid(fold, max, rowPx, pickedWeek = days.indexOf(state.anchor) / DAYS_IN_WEEK) {
        MonthWeeks(YearMonth.from(state.anchor), state.anchor, ctx.today, { state.anchor = it }, MonthSize) { day ->
            tasks[day].orEmpty().take(MONTH_DOTS).map { dotColors.getValue(it.priority) }
        }
    }
    Legend()
    val picked = tasks[state.anchor].orEmpty()
    val pickedDone = done[state.anchor] ?: DoneDay.None
    LazyColumn(
        Modifier.fillMaxSize().nestedScroll(connection),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        item { PickedDay(state.anchor, ctx.today, picked.size) }
        if (picked.isEmpty() && pickedDone.tasks.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.calendar_nothing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.tokens.ink2,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
        items(picked, key = { it.id }) { task -> TaskItem(task, ctx, ctx.meta(task, showDate = false)) }
        doneSection(pickedDone, ctx)
    }
}

/** What the month's dots mean: the three priorities, no priority, and that done tasks have none. */
@Composable
private fun Legend() {
    val tokens = MaterialTheme.tokens
    val style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
    FlowRow(
        Modifier.fillMaxWidth().rowDivider(tokens.hairline, start = 0.dp)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(Priority.P1, Priority.P2, Priority.P3).forEach { level ->
            LegendDot(priorityColor(level), stringResource(R.string.priority_short, level.level), style)
        }
        LegendDot(tokens.ink3, stringResource(R.string.priority_none), style)
        Text(stringResource(R.string.calendar_legend_done), style = style, color = tokens.ink2)
    }
}

@Composable
private fun LegendDot(color: Color, label: String, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = style, color = MaterialTheme.tokens.ink2)
    }
}

/** "Today  Sat 21 Nov · 3 open": the picked day's name in bold, then its date and open count. */
@Composable
private fun PickedDay(day: LocalDate, today: LocalDate, open: Int) {
    val tokens = MaterialTheme.tokens
    val near = abs(ChronoUnit.DAYS.between(today, day)) <= 1
    val date = sectionDate(day, today)
    val rest = listOfNotNull(
        date.takeIf { near },
        pluralStringResource(R.plurals.list_open_count, open, open).takeIf { open > 0 },
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (near) dayLabel(day, today) else date,
            style = MaterialTheme.typography.dayHeader,
            color = tokens.ink,
            modifier = Modifier.alignByBaseline(),
        )
        if (rest.isNotEmpty()) {
            Text(
                rest,
                style = MaterialTheme.typography.meta,
                color = tokens.ink2,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/**
 * What was ticked off on the picked day, under its open tasks: "Done · 3 · 1 h 20 min focused", then
 * each task crossed out with when it was done and its focus that day. Tapping one opens it, where it can
 * be reopened.
 */
private fun LazyListScope.doneSection(done: DoneDay, ctx: RowContext) {
    if (done.tasks.isEmpty()) return
    item(key = "done") { DoneHeader(done) }
    // Keyed apart from the open rows, so a task reopened a moment ago never shares a key with itself.
    items(done.tasks, key = { "done-${it.task.id}" }) { DoneRow(it, ctx) }
}

/** A quiet line, like the folded "Completed" one elsewhere, so done tasks don't compete with open ones. */
@Composable
private fun DoneHeader(done: DoneDay) {
    val count = done.tasks.size
    Text(
        if (done.focused.toMinutes() > 0) {
            stringResource(R.string.calendar_done_focused, count, focusDuration(done.focused))
        } else {
            stringResource(R.string.calendar_done, count)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.tokens.ink2,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
    )
}

/** A done task: its ticked box, its title crossed out and "Done 3:12 PM · Focused 45 min". */
@Composable
private fun DoneRow(done: DoneTask, ctx: RowContext) {
    val task = done.task
    val muted = MaterialTheme.tokens.ink2
    Row(
        Modifier.fillMaxWidth().rowDivider(MaterialTheme.tokens.hairline)
            .clickable { if (ctx.selection.active) ctx.selection.toggle(task.id) else ctx.open(task.id) }
            .heightIn(min = 48.dp).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        RoundCheck(checked = true, onClick = { ctx.toggle(task) }, priority = task.priority)
        Spacer(Modifier.width(2.dp))
        Column(
            Modifier.weight(1f).padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.taskTitle,
                color = muted,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(doneMeta(done), style = MaterialTheme.typography.meta, color = muted, maxLines = 1)
        }
    }
}

/** "Done 3:12 PM · Focused 45 min", or just "Done 3:12 PM" without focus that day. */
@Composable
private fun doneMeta(done: DoneTask): String {
    val at = timeLabel(done.at)
    return if (done.focused.toMinutes() > 0) {
        stringResource(R.string.calendar_done_at_focused, at, focusDuration(done.focused))
    } else {
        stringResource(R.string.calendar_done_at, at)
    }
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

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
