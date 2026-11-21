package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.section
import com.aliraza.dailytodos.ui.theme.sheetTitle
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/*
 * Ticking a task: the ring fills with the accent and swells to PULSE_SCALE for PULSE_MS, holds the accent
 * for a beat, then the task is marked done at TICK_SETTLE_MS and the ring settles to the neutral done grey.
 * With reduced motion there is no swell and the ring goes straight to grey.
 */
private const val PULSE_MS = 180
private const val PULSE_SCALE = 1.12f
private const val CHECK_DRAW_MS = 180
private const val TICK_SETTLE_MS = 600L

/** The check inside a ring: the design's 24-unit check drawn at 14 dp in the middle of the 24 dp ring. */
private val CHECK_POINTS = listOf(Offset(5f, 12.5f), Offset(9.5f, 17f), Offset(19f, 7.5f))
private const val CHECK_GRID = 24f
private val CHECK_SIZE = 14.dp
private val CHECK_STROKE = 1.75.dp

private const val PRIORITY_TINT = 0.1f
private const val HALF_TURN = 180f

/** The ring's touch target. */
private val CHECK_TARGET = 48.dp

/** Where row text starts, so hairlines line up under it rather than under the ring. */
val RowTextStart: Dp = 58.dp

/** The ring colour for a priority: red, orange and blue for P1 to P3, ink-3 for none. */
@Composable
fun priorityColor(priority: Priority): Color = with(MaterialTheme.tokens) {
    when (priority) {
        Priority.P1 -> priority1
        Priority.P2 -> priority2
        Priority.P3 -> priority3
        Priority.NONE -> ink3
    }
}

/** A due date's colour: red only when overdue, ink for today, grey for a passed time and later days. */
@Composable
fun toneColor(tone: DateTone): Color = with(MaterialTheme.tokens) {
    when (tone) {
        DateTone.OVERDUE -> danger
        DateTone.TODAY -> ink
        DateTone.PASSED, DateTone.TOMORROW, DateTone.THIS_WEEK, DateTone.LATER -> ink2
    }
}

/** Today's date is set in semi-bold, so it stands out by weight rather than colour. */
fun toneWeight(tone: DateTone): FontWeight = if (tone == DateTone.TODAY) FontWeight.SemiBold else FontWeight.Medium

/**
 * A round check box whose ring shows the priority, so it reads before the title does: ink-3 for none,
 * a heavier coloured ring with a faint tint for P1 to P3. Ticked, it is filled ink-3, the same for every
 * priority, since done is neutral. While [flashing], the moment between the tap and the task being
 * marked done, it fills with the accent and swells once. Ticking gives a short haptic tick.
 */
@Composable
fun RoundCheck(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    priority: Priority = Priority.NONE,
    shape: Shape = CircleShape,
    size: Dp = 24.dp,
    flashing: Boolean = false,
) {
    val tokens = MaterialTheme.tokens
    val still = reducedMotion()
    val ring = priorityColor(priority)
    val accentFill = flashing && !still
    val fillTarget = when {
        accentFill -> tokens.accent
        checked || flashing -> tokens.ink3
        priority == Priority.NONE -> Color.Transparent
        else -> ring.copy(alpha = PRIORITY_TINT)
    }
    val fill by animateColorAsState(fillTarget, label = "checkFill")
    val edge by animateColorAsState(
        when {
            accentFill -> tokens.accent
            checked || flashing -> tokens.ink3
            else -> ring
        },
        label = "checkEdge",
    )
    val mark = if (accentFill) tokens.onAccent else tokens.background
    val scale = remember { Animatable(1f) }
    LaunchedEffect(accentFill) {
        if (accentFill) {
            scale.animateTo(PULSE_SCALE, tween(PULSE_MS / 2))
            scale.animateTo(1f, tween(PULSE_MS / 2))
        }
    }
    val weight = if (priority == Priority.NONE || checked || flashing) 2.dp else 2.5.dp
    val label = checkLabel(checked, priority)
    val tick = rememberTick(HapticFeedbackType.Confirm)
    Box(
        modifier = modifier
            .size(CHECK_TARGET)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox) {
                if (!checked) tick()
                onClick()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(shape)
                .background(fill)
                .border(weight, edge, shape),
            contentAlignment = Alignment.Center,
        ) {
            CheckMark(checked || flashing, mark)
        }
    }
}

/**
 * The marker that replaces the ring while selecting: a rounded square, since circles mean done and
 * squares mean selected. Selected, it is the solid accent with an on-accent check.
 */
@Composable
fun SelectionMarker(selected: Boolean, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.tokens
    val fill by animateColorAsState(if (selected) tokens.accent else Color.Transparent, label = "selFill")
    val edge by animateColorAsState(if (selected) tokens.accent else tokens.ink3, label = "selEdge")
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.size(CHECK_TARGET), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(24.dp).clip(shape).background(fill).border(2.dp, edge, shape),
            contentAlignment = Alignment.Center,
        ) {
            CheckMark(selected, tokens.onAccent)
        }
    }
}

/** What a row shows under the title. Null parts are left out. */
data class RowMeta(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    /** The list's name; callers leave it out for the default list, so only other lists show. */
    val listName: String? = null,
    /** How near the task's due date is; colours the date and time. */
    val tone: DateTone = DateTone.LATER,
    /** Ticked and total sub-tasks, shown as "1/3"; null when the task has none. */
    val subtasks: Pair<Int, Int>? = null,
    /** When a done task was ticked: its row shows "Done 5:48 PM" in place of the due date. */
    val doneAt: LocalTime? = null,
    /** Time focused on the task, already written ("25 min focus"); null leaves it out. */
    val focus: String? = null,
    /** How long ago a time passed earlier today: "4:00 PM · 2 h ago". Null for any other task. */
    val passedFor: Duration? = null,
)

/** A hairline along the bottom edge, starting under the row text. */
fun Modifier.rowDivider(color: Color, start: Dp = RowTextStart): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(start.toPx(), y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

/**
 * One task: the ring, the title and one quiet meta line. Tap opens it, long-press starts multi-select.
 * While [selecting], the ring becomes the selection marker and a selected row takes the accent
 * container tint. With a [fold], the meta line's "1/3" folds the sub-tasks open under the row.
 * [trailing] holds the drag handle in a list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    meta: RowMeta,
    selected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selecting: Boolean = false,
    fold: SubtaskFold? = null,
    title: AnnotatedString? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val done = task.completedAt != null
    var ticking by remember(task.id, done) { mutableStateOf(false) }
    val toggle by rememberUpdatedState(onToggle)
    LaunchedEffect(ticking) {
        if (ticking) {
            delay(TICK_SETTLE_MS)
            toggle()
        }
    }
    val background by animateColorAsState(
        if (selected) MaterialTheme.tokens.accentContainer else Color.Transparent,
        label = "selected",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .rowDivider(MaterialTheme.tokens.hairline)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 56.dp)
            .padding(start = 8.dp, end = 12.dp),
    ) {
        Leading(selecting, selected, done, ticking, task.priority) {
            when {
                done -> onToggle()
                !ticking -> ticking = true
            }
        }
        Spacer(Modifier.width(RowTextStart - 8.dp - CHECK_TARGET))
        Column(
            Modifier.weight(1f).padding(top = 12.dp, bottom = 12.dp, end = 8.dp),
        ) {
            Text(
                text = title ?: AnnotatedString(task.title),
                style = MaterialTheme.typography.taskTitle,
                color = if (done) MaterialTheme.tokens.ink2 else MaterialTheme.tokens.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(today, meta, done, hasNotes = task.notes.isNotBlank(), fold = fold)
            if (fold?.open == true) fold.content()
        }
        Box(Modifier.align(Alignment.CenterVertically)) { Row(content = trailing) }
    }
}

/** The row's start: the ring, or the selection marker while selecting. [onTap] ticks or reopens. */
@Composable
private fun Leading(
    selecting: Boolean,
    selected: Boolean,
    done: Boolean,
    ticking: Boolean,
    priority: Priority,
    onTap: () -> Unit,
) {
    if (selecting) {
        SelectionMarker(selected)
    } else {
        RoundCheck(checked = done, onClick = onTap, priority = priority, flashing = ticking && !done)
    }
}

/** "Complete, Priority 1": what the box does, and the task's priority when it has one. */
@Composable
private fun checkLabel(checked: Boolean, priority: Priority): String {
    val action = stringResource(if (checked) R.string.action_reopen else R.string.action_complete)
    if (priority == Priority.NONE) return action
    return "$action, ${stringResource(R.string.priority_long, priority.level)}"
}

/** The check, drawing itself in as the box is ticked and out as it is cleared. */
@Composable
private fun CheckMark(checked: Boolean, color: Color) {
    val drawn by animateFloatAsState(if (checked) 1f else 0f, tween(CHECK_DRAW_MS), label = "check")
    if (drawn > 0f) Canvas(Modifier.size(CHECK_SIZE)) { drawCheck(drawn, color) }
}

/** The check mark, drawn up to [progress] of its length. */
private fun DrawScope.drawCheck(progress: Float, color: Color) {
    val points = CHECK_POINTS.map { Offset(it.x / CHECK_GRID * size.width, it.y / CHECK_GRID * size.height) }
    val segments = points.zipWithNext()
    var left = segments.sumOf { (a, b) -> (b - a).getDistance().toDouble() }.toFloat() * progress
    val stroke = CHECK_STROKE.toPx()
    segments.forEach { (a, b) ->
        if (left <= 0f) return
        val length = (b - a).getDistance()
        val end = if (left >= length) b else a + (b - a) * (left / length)
        drawLine(color, a, end, stroke, StrokeCap.Round)
        left -= length
    }
}

/**
 * The meta line: when it is due (or when it was done), its list, its sub-tasks and its focus time,
 * each with a 14 dp icon, wrapping onto a second line when the row is narrow.
 */
@Composable
private fun MetaLine(today: LocalDate, meta: RowMeta, done: Boolean, hasNotes: Boolean, fold: SubtaskFold?) {
    val whenText = whenText(today, meta, done)
    val hasAny = whenText != null || meta.listName != null || meta.subtasks != null || meta.focus != null || hasNotes
    if (!hasAny) return
    FlowRow(
        Modifier.padding(top = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        whenText?.let { WhenItem(it, meta, done) }
        meta.subtasks?.let { (ticked, total) -> SubtaskCount(ticked, total, fold) }
        meta.listName?.let { MetaItem(AppIcons.Lists, it) }
        meta.focus?.let { MetaItem(AppIcons.Timer, it) }
        if (hasNotes && !done) MetaItem(AppIcons.Notes, null)
    }
}

/** "Done 5:48 PM" for a done task; else "Fri 27 Nov 4:00 PM", with " · 2 h ago" for a time passed today. */
@Composable
private fun whenText(today: LocalDate, meta: RowMeta, done: Boolean): String? {
    if (done) return meta.doneAt?.let { stringResource(R.string.row_done_at, timeLabel(it)) }
    val due = listOfNotNull(meta.date?.let { dayLabel(it, today) }, meta.time?.let { timeLabel(it) })
        .joinToString(" ")
        .ifEmpty { return null }
    return meta.passedFor?.let { "$due · ${agoLabel(it)}" } ?: due
}

/** The when part of the meta line, coloured by its tone; a done task's time is plain and has no icon. */
@Composable
private fun WhenItem(text: String, meta: RowMeta, done: Boolean) {
    val tone = if (done) DateTone.LATER else meta.tone
    val icon = when {
        done -> null
        meta.date == null -> AppIcons.Clock
        else -> AppIcons.Calendar
    }
    MetaItem(icon, text, color = toneColor(tone), weight = toneWeight(tone))
}

/** "12 min ago" under an hour, then "2 h ago". */
@Composable
private fun agoLabel(passed: Duration): String {
    val minutes = passed.toMinutes().coerceAtLeast(1)
    return if (minutes < MINUTES_PER_HOUR) {
        stringResource(R.string.row_ago_minutes, minutes)
    } else {
        stringResource(R.string.row_ago_hours, minutes / MINUTES_PER_HOUR)
    }
}

private const val MINUTES_PER_HOUR = 60L

/** One part of the meta line: a 14 dp icon and its text, in ink-2 unless a tone says otherwise. */
@Composable
fun MetaItem(
    icon: ImageVector?,
    text: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.tokens.ink2,
    weight: FontWeight = FontWeight.Medium,
) {
    Row(modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(14.dp)) }
        if (icon != null && text != null) Spacer(Modifier.width(4.dp))
        text?.let {
            Text(it, style = MaterialTheme.typography.meta, color = color, fontWeight = weight, maxLines = 1)
        }
    }
}

/** A task's sub-tasks, folded under its row: [open] shows them, [onToggle] flips it, [content] draws them. */
class SubtaskFold(val open: Boolean, val onToggle: () -> Unit, val content: @Composable () -> Unit)

/**
 * "1/3" in the meta line. With a [fold] it is a button that folds the sub-tasks open or shut in place,
 * without opening the task, and a small chevron shows which way it goes.
 */
@Composable
private fun SubtaskCount(done: Int, total: Int, fold: SubtaskFold?) {
    val text = "$done/$total"
    if (fold == null) {
        MetaItem(AppIcons.Subtasks, text)
        return
    }
    val turn by animateFloatAsState(if (fold.open) HALF_TURN else 0f, label = "fold")
    val progress = stringResource(R.string.subtasks_progress_label, done, total)
    val action = stringResource(if (fold.open) R.string.subtasks_hide else R.string.subtasks_show)
    Row(
        Modifier.clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClickLabel = action, onClick = fold.onToggle)
            .semantics(mergeDescendants = true) { contentDescription = "$progress. $action" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetaItem(AppIcons.Subtasks, text)
        Icon(
            AppIcons.ChevronDown,
            null,
            tint = MaterialTheme.tokens.ink2,
            modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

/** A header's one action, a text button at its right edge: "Reschedule", "Hide". [label] is for screen readers. */
class HeaderAction(val text: String, val icon: ImageVector? = null, val label: String = text, val onClick: () -> Unit)

/**
 * A section header: an 18 sp extra-bold title with its [count] beside it, and at the right either an
 * [action] or a quiet [trailing] note ("2 done"). No hairline: the space above it does the splitting.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailing: String? = null,
    action: HeaderAction? = null,
) {
    val tokens = MaterialTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 28.dp - HEADER_SLACK, bottom = 8.dp - HEADER_SLACK)
            .heightIn(min = HEADER_LINE + HEADER_SLACK * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The title and its count take what the action leaves, so the action stays at the right edge.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.section,
                color = tokens.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            count?.let {
                Text(
                    it.toString(),
                    style = MaterialTheme.typography.meta,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.ink2,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        when {
            action != null -> HeaderButton(action)

            trailing != null -> Text(
                trailing,
                style = MaterialTheme.typography.meta,
                fontWeight = FontWeight.SemiBold,
                color = tokens.ink2,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

/** The header's text line, and the room a 40 dp button takes out of its padding so every header is as tall. */
private val HEADER_LINE = 24.dp
private val HEADER_SLACK = 8.dp

@Composable
private fun HeaderButton(action: HeaderAction) {
    val accent = MaterialTheme.tokens.accent
    Row(
        Modifier.height(HEADER_LINE + HEADER_SLACK * 2).clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = action.label, onClick = action.onClick)
            .semantics(mergeDescendants = true) { contentDescription = action.label }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        action.icon?.let {
            Icon(it, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(action.text, style = MaterialTheme.typography.button, color = accent)
    }
}

/** A button under an empty state's message: "See tomorrow". */
class EmptyAction(val text: String, val onClick: () -> Unit)

/**
 * A calm, centred message for a screen with nothing to show: the app's mark (or another [icon]) in an
 * accent-container circle, a title, a line of detail and an optional tonal [action]. It takes the page's
 * [padding] and centres inside it: the room under it includes an open keyboard, so the message moves up
 * above the keyboard rather than being cut off by it.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Mark,
    action: EmptyAction? = null,
) {
    val tokens = MaterialTheme.tokens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.padding(bottom = 8.dp).size(88.dp).clip(CircleShape).background(tokens.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            val markSize = if (icon == AppIcons.Mark) 56.dp else 36.dp
            Icon(icon, null, tint = tokens.accent, modifier = Modifier.size(markSize))
        }
        Text(
            title,
            style = MaterialTheme.typography.sheetTitle,
            fontWeight = FontWeight.ExtraBold,
            color = tokens.ink,
            textAlign = TextAlign.Center,
        )
        Text(body, style = MaterialTheme.typography.bodyMedium, color = tokens.ink2, textAlign = TextAlign.Center)
        action?.let { TonalButton(it.text, it.onClick, Modifier.padding(top = 8.dp)) }
    }
}
