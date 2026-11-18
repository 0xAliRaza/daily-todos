package com.aliraza.dailytasks.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.Priority
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/** How long a ticked box stays visible before the task leaves the list. */
private const val TICK_SETTLE_MS = 280L

/** A prioritised box is tinted with its own colour at this strength. */
private const val PRIORITY_TINT = 0.12f

/** The outer ring of the empty-state mark is the accent tint at half strength. */
private const val EMPTY_HALO = 0.5f

private const val HALF_TURN = 180f

/** Where row text starts, so hairlines line up under it rather than under the check box. */
val RowTextStart: Dp = 52.dp

/** The ring colour for a priority: red, orange and blue for P1 to P3, grey for none. */
@Composable
fun priorityColor(priority: Priority): Color = with(MaterialTheme.tokens) {
    when (priority) {
        Priority.P1 -> priority1
        Priority.P2 -> priority2
        Priority.P3 -> priority3
        Priority.NONE -> outline
    }
}

@Composable
fun toneColor(tone: DateTone): Color = with(MaterialTheme.tokens) {
    when (tone) {
        DateTone.OVERDUE -> overdue
        DateTone.TODAY -> accentText
        DateTone.TOMORROW, DateTone.THIS_WEEK, DateTone.LATER -> onSurfaceVariant
    }
}

/**
 * A round check box whose ring shows the priority, so it reads before the title does. A ticked box
 * fills with the same colour.
 */
@Composable
fun RoundCheck(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    priority: Priority = Priority.NONE,
    shape: Shape = CircleShape,
    size: Int = 22,
) {
    val ring = priorityColor(priority)
    val idle = if (priority == Priority.NONE) Color.Transparent else ring.copy(alpha = PRIORITY_TINT)
    val fill by animateColorAsState(if (checked) ring else idle, label = "check")
    val action = stringResource(if (checked) R.string.action_reopen else R.string.action_complete)
    val level = stringResource(R.string.priority_long, priority.level)
    val label = if (priority == Priority.NONE) action else "$action, $level"
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(shape)
                .background(fill)
                .border(1.5.dp, ring, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(AppIcons.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize().padding(3.dp))
            }
        }
    }
}

/** What a row shows under the title. Null parts are left out. */
data class RowMeta(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val listName: String? = null,
    /** How near the task's due date is; colours the date and time. */
    val tone: DateTone = DateTone.LATER,
    /** Ticked and total sub-tasks, shown as "1/3"; null when the task has none. */
    val subtasks: Pair<Int, Int>? = null,
)

/** A hairline along the bottom edge, starting under the row text. */
fun Modifier.rowDivider(color: Color, start: Dp = RowTextStart): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(start.toPx(), y), Offset(size.width, y), strokeWidth = 1.dp.toPx() / 2)
}

/**
 * One task: check box, title, a line of notes and a quiet meta line. Tap opens it, long-press starts
 * multi-select. With a [fold] its sub-tasks open under it from the "3/4" at the right. [trailing] holds
 * the drag handle in a list.
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
    fold: SubtaskFold? = null,
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
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "selected",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .rowDivider(MaterialTheme.tokens.divider)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 48.dp)
            .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        RoundCheck(
            checked = done != ticking,
            onClick = { if (!ticking) ticking = true },
            priority = task.priority,
        )
        Spacer(Modifier.width(4.dp))
        RowText(task, today, meta, fold, Modifier.weight(1f))
        if (fold != null && meta.subtasks != null) {
            val (ticked, total) = meta.subtasks
            Box(Modifier.padding(top = 2.dp)) { SubtaskToggle(ticked, total, fold.open, fold.onToggle) }
        }
        Box(Modifier.align(Alignment.CenterVertically)) { Row(content = trailing) }
    }
}

/** Title, a line of notes, the meta line, and the sub-tasks when folded open. */
@Composable
private fun RowText(task: Task, today: LocalDate, meta: RowMeta, fold: SubtaskFold?, modifier: Modifier) {
    Column(modifier.padding(top = 9.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Title(task.title, task.completedAt != null)
        if (task.notes.isNotBlank()) Notes(task.notes)
        MetaLine(today, meta)
        if (fold?.open == true) fold.content()
    }
}

@Composable
private fun Title(title: String, done: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = with(MaterialTheme.colorScheme) { if (done) onSurfaceVariant else onBackground },
        textDecoration = if (done) TextDecoration.LineThrough else null,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Notes(notes: String) {
    Text(
        text = notes.lineSequence().first { it.isNotBlank() }.trim(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MetaLine(today: LocalDate, meta: RowMeta) {
    val time = meta.time?.let(::timeLabel)
    val whenText = listOfNotNull(meta.date?.let { dayLabel(it, today) }, time).joinToString(" ")
    if (whenText.isEmpty() && meta.listName == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (whenText.isNotEmpty()) {
                val ink = toneColor(meta.tone)
                Icon(
                    if (meta.date == null) AppIcons.Clock else AppIcons.Today,
                    null,
                    tint = ink,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(whenText, style = MaterialTheme.typography.bodySmall, color = ink, maxLines = 1)
            }
        }
        meta.listName?.let { name ->
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.width(4.dp))
            Icon(AppIcons.Lists, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
        }
    }
}

/** A task's sub-tasks, folded under its row: [open] shows them, [onToggle] flips it, [content] draws them. */
class SubtaskFold(val open: Boolean, val onToggle: () -> Unit, val content: @Composable () -> Unit)

/**
 * "3/4" and a chevron at the row's right edge. Tapping it folds the sub-tasks open or shut in place,
 * without opening the task. All ticked reads in the accent.
 */
@Composable
private fun SubtaskToggle(done: Int, total: Int, open: Boolean, onToggle: () -> Unit) {
    val ink = if (done == total) MaterialTheme.tokens.accentText else MaterialTheme.colorScheme.onSurfaceVariant
    val turn by animateFloatAsState(if (open) HALF_TURN else 0f, label = "fold")
    val progress = stringResource(R.string.subtasks_progress_label, done, total)
    val action = stringResource(if (open) R.string.subtasks_hide else R.string.subtasks_show)
    Row(
        Modifier.heightIn(min = 40.dp).clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onToggle)
            .semantics(mergeDescendants = true) { contentDescription = "$progress. $action" }
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$done/$total", style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1)
        Icon(
            AppIcons.ChevronDown,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

/** A bold group title with a hairline under it, the way lists are split into days and sections. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, accent: Boolean = false, trailing: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .rowDivider(MaterialTheme.tokens.divider, start = 16.dp)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (accent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Two soft rings around a check: calm, and recognisably the app's own mark.
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = EMPTY_HALO)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(
                    72.dp,
                ).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
