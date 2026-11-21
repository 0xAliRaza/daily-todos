package com.aliraza.dailytodos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.DeviceClock
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.SubtaskProgress
import com.aliraza.dailytodos.ui.AddButtonClearance
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.DateTone
import com.aliraza.dailytodos.ui.components.EmptyState
import com.aliraza.dailytodos.ui.components.HeaderAction
import com.aliraza.dailytodos.ui.components.RoundCheck
import com.aliraza.dailytodos.ui.components.RowMeta
import com.aliraza.dailytodos.ui.components.SectionHeader
import com.aliraza.dailytodos.ui.components.SubtaskFold
import com.aliraza.dailytodos.ui.components.SwipeRow
import com.aliraza.dailytodos.ui.components.TaskRow
import com.aliraza.dailytodos.ui.components.dueTone
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Multi-select: a long-press starts it, and while it runs a tap toggles a task instead of opening it. */
@Stable
class Selection {
    var ids by mutableStateOf(emptySet<String>())
        private set
    val active: Boolean get() = ids.isNotEmpty()

    fun toggle(id: String) {
        ids = if (id in ids) ids - id else ids + id
    }

    fun clear() {
        ids = emptySet()
    }
}

/** Which list names a row's meta line shows. */
enum class ListLabel {
    /** None: the screen is the list itself. */
    HIDDEN,

    /** Every list but the default one, so most rows stay one quiet line. */
    OTHER_LISTS,

    /** Every list, the default one too: search, where a task could be anywhere. */
    ALWAYS,
}

/** What every task row needs from the screen around it. */
class RowContext(
    val now: LocalDateTime,
    val lists: Map<String, TaskList>,
    val selection: Selection,
    val open: (String) -> Unit,
    val toggle: (Task) -> Unit,
    val subtasks: Map<String, SubtaskProgress> = emptyMap(),
    val subtasksOf: (String) -> Flow<List<Subtask>> = { flowOf(emptyList()) },
    val setSubtaskDone: (Subtask, Boolean) -> Unit = { _, _ -> },
    val delete: (Task) -> Unit = {},
    val reschedule: (List<String>) -> Unit = {},
    val focus: (String) -> Unit = {},
    /** Turns a done task's instant into the phone's wall-clock time. */
    val zone: ZoneId = DeviceClock.zone,
) {
    val today: LocalDate get() = now.toLocalDate()

    /**
     * A task's meta line. Without [showDate] only its time shows, as on Today and under a day's header;
     * a done task shows when it was ticked, if that was today.
     */
    fun meta(task: Task, showDate: Boolean, list: ListLabel = ListLabel.OTHER_LISTS): RowMeta {
        val taskList = lists[task.listId]
        val showList = when (list) {
            ListLabel.HIDDEN -> false
            ListLabel.OTHER_LISTS -> taskList?.isDefault == false
            ListLabel.ALWAYS -> true
        }
        val tone = task.dueTone(now) ?: DateTone.LATER
        return RowMeta(
            date = task.dueDate?.takeIf { showDate },
            time = task.dueTime,
            listName = taskList?.name?.takeIf { showList },
            tone = tone,
            subtasks = subtasks[task.id]?.let { it.done to it.total },
            doneAt = task.completedAt?.atZone(zone)?.takeIf { it.toLocalDate() == today }?.toLocalTime(),
            passedFor = task.dueTime?.takeIf { tone == DateTone.PASSED }
                ?.let { Duration.between(it, now.toLocalTime()) },
        )
    }
}

@Composable
fun rememberRowContext(
    viewModel: TasksViewModel,
    selection: Selection,
    onReschedule: (List<String>) -> Unit,
    onFocus: (String) -> Unit,
    open: (String) -> Unit,
): RowContext {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val progress by viewModel.subtaskProgress.collectAsStateWithLifecycle()
    return remember(now, lists, selection, open, progress, onReschedule, onFocus) {
        RowContext(
            now,
            lists.associateBy { it.id },
            selection,
            open,
            toggle = { task -> viewModel.setDone(listOf(task.id), task.completedAt == null) },
            subtasks = progress,
            subtasksOf = viewModel::observeSubtasks,
            setSubtaskDone = viewModel::setSubtaskDone,
            delete = { task -> viewModel.delete(listOf(task.id)) },
            reschedule = onReschedule,
            focus = onFocus,
        )
    }
}

/**
 * A task row wired to the screen's selection and sheet. Its sub-tasks start folded; the "1/3" in the
 * meta line folds them open in place. A done row never folds open.
 */
@Composable
internal fun TaskItem(
    task: Task,
    ctx: RowContext,
    meta: RowMeta,
    modifier: Modifier = Modifier,
    title: AnnotatedString? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val done = task.completedAt != null
    var open by rememberSaveable(task.id) { mutableStateOf(false) }
    val fold = meta.subtasks?.takeIf { !done }?.let {
        SubtaskFold(open, onToggle = { open = !open }) { InlineSubtasks(task.id, ctx) }
    }
    SwipeRow(
        enabled = !ctx.selection.active,
        canFocus = !done,
        onFocus = { ctx.focus(task.id) },
        onDelete = { ctx.delete(task) },
        modifier = modifier,
    ) {
        TaskRow(
            task = task,
            today = ctx.today,
            meta = meta,
            selected = task.id in ctx.selection.ids,
            onToggle = { ctx.toggle(task) },
            onClick = { if (ctx.selection.active) ctx.selection.toggle(task.id) else ctx.open(task.id) },
            onLongClick = { ctx.selection.toggle(task.id) },
            selecting = ctx.selection.active,
            fold = fold,
            title = title,
            trailing = trailing,
        )
    }
}

/**
 * The sub-tasks under an open row, one step in from the title, with a hairline rail down their left
 * that ties them to their task. Tap one, box or text, to tick it. New ones are added in the task sheet.
 */
@Composable
private fun InlineSubtasks(taskId: String, ctx: RowContext) {
    val subtasks by remember(taskId) { ctx.subtasksOf(taskId) }.collectAsStateWithLifecycle(emptyList())
    val tokens = MaterialTheme.tokens
    Column(Modifier.padding(top = 4.dp).subtaskRail(tokens.hairline)) {
        subtasks.forEach { sub ->
            Row(
                Modifier.padding(start = RAIL_INSET).fillMaxWidth().heightIn(min = SUBTASK_HEIGHT)
                    .clip(RoundedCornerShape(8.dp)).clickable { ctx.setSubtaskDone(sub, !sub.done) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundCheck(
                    checked = sub.done,
                    onClick = { ctx.setSubtaskDone(sub, !sub.done) },
                    shape = RoundedCornerShape(4.dp),
                    size = 16.dp,
                )
                Text(
                    sub.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sub.done) tokens.ink2 else tokens.ink,
                    textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** How far the sub-tasks sit in from the rail, which runs under the title's first letter. */
private val RAIL_INSET = 6.dp
private val RAIL_WIDTH = 1.5.dp
private val SUBTASK_HEIGHT = 40.dp

/** The rail: a short rounded line down the block's left edge, kept clear of its first and last row's ends. */
private fun Modifier.subtaskRail(color: Color): Modifier = drawBehind {
    val inset = 10.dp.toPx()
    val x = RAIL_WIDTH.toPx() / 2
    drawLine(color, Offset(x, inset), Offset(x, size.height - inset), RAIL_WIDTH.toPx(), StrokeCap.Round)
}

/** A titled group of task rows with its count, and at the right an [action] or a quiet [trailing] note. */
internal fun LazyListScope.section(
    title: Int,
    tasks: List<Task>,
    ctx: RowContext,
    showDate: Boolean = false,
    action: HeaderAction? = null,
    trailing: String? = null,
) {
    if (tasks.isEmpty()) return
    item(key = "h$title") {
        SectionHeader(
            stringResource(title),
            Modifier.animateItem(),
            count = tasks.size,
            trailing = trailing,
            action = action,
        )
    }
    items(tasks, key = { it.id }) { task -> TaskItem(task, ctx, ctx.meta(task, showDate), Modifier.animateItem()) }
}

// Completed

private val COMPLETED_BAR_HEIGHT = 60.dp

/** The bar's right end stays clear for the + button, which sits over it. */
private val COMPLETED_BAR_END = 92.dp

/**
 * A page whose done tasks fold into a "Completed 6" bar pinned at its bottom, on the tab bar, rather than
 * floating after the last row. [pinned] shows the bar (when anything is [done]); tapping it calls [onOpen].
 * [content] gets the padding its list needs: the page's top, and room above the bar or the page's bottom.
 */
@Composable
internal fun PinnedCompletedPage(
    padding: PaddingValues,
    done: Int,
    pinned: Boolean,
    onOpen: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val top = padding.calculateTopPadding()
    val bottom = padding.calculateBottomPadding()
    val showBar = pinned && done > 0
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // With the bar, the + button reaches only a little above it, so the list needs just that room.
            val room = if (showBar) AddButtonClearance - COMPLETED_BAR_HEIGHT else bottom
            content(PaddingValues(top = top, bottom = room.coerceAtLeast(0.dp)))
        }
        if (showBar) {
            CompletedBar(done, onOpen)
            // The page's bottom padding holds the + button's clearance; the bar sits below that, on the tabs.
            Spacer(Modifier.height((bottom - AddButtonClearance).coerceAtLeast(0.dp)))
        }
    }
}

/** "Completed 6 ⌃" over a top hairline: tapping it opens the done tasks in the list above. */
@Composable
private fun CompletedBar(count: Int, onOpen: () -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().background(tokens.background).topHairline(tokens.hairline)
            .clickable(onClickLabel = stringResource(R.string.completed_show), onClick = onOpen)
            .height(COMPLETED_BAR_HEIGHT)
            .padding(start = 20.dp, end = COMPLETED_BAR_END),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.completed_label),
            style = MaterialTheme.typography.taskTitle,
            fontWeight = FontWeight.Bold,
            color = tokens.ink,
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.meta,
            fontWeight = FontWeight.SemiBold,
            color = tokens.ink2,
        )
        Icon(AppIcons.ChevronUp, null, tint = tokens.ink2, modifier = Modifier.size(20.dp))
    }
}

private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}

/**
 * The opened done tasks: a "Completed 6" header with Hide, then the done rows, crossed out with when
 * each was ticked. Ticking one again reopens it and it goes back up.
 */
internal fun LazyListScope.completedSection(done: List<Task>, ctx: RowContext, list: ListLabel, onHide: () -> Unit) {
    if (done.isEmpty()) return
    item(key = "completed") {
        SectionHeader(
            stringResource(R.string.completed_label),
            Modifier.animateItem(),
            count = done.size,
            action = HeaderAction(
                stringResource(R.string.completed_hide),
                AppIcons.ChevronDown,
                stringResource(R.string.completed_hide_label),
                onHide,
            ),
        )
    }
    items(done, key = { it.id }) { task ->
        TaskItem(task, ctx, ctx.meta(task, showDate = false, list = list), Modifier.animateItem())
    }
}

// Lists

@Composable
fun ListsContent(viewModel: TasksViewModel, padding: PaddingValues, onOpen: (String) -> Unit) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val counts by viewModel.listCounts.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        items(lists, key = { it.id }) { list ->
            ListRow(list, counts[list.id] ?: 0, Modifier.animateItem()) { onOpen(list.id) }
        }
        item(key = "new") { NewListField(viewModel::createList) }
    }
}

@Composable
private fun ListRow(list: TaskList, open: Int, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .rowDivider(MaterialTheme.tokens.hairline, start = ListTextStart)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = ListTextStart, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(list.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The one list that can't be deleted says what it is for, in words rather than a symbol.
            if (list.isDefault) {
                Text(
                    stringResource(R.string.list_default_mark),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (open > 0) {
            Text(open.toString(), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
        Icon(AppIcons.ChevronRight, null, tint = scheme.outline, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Where list names start, in line with the page title; the hairlines under them start there too. */
private val ListTextStart = 16.dp

@Composable
private fun NewListField(onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = ListTextStart, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Add, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) {
            if (name.isEmpty()) {
                Text(
                    stringResource(R.string.list_new),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onBackground),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onCreate(name)
                    name = ""
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * One list in manual order, with its done tasks behind the pinned Completed bar. While selecting, each
 * row gets a drag handle at its right.
 */
@Composable
fun ListContent(viewModel: TasksViewModel, listId: String, ctx: RowContext, padding: PaddingValues) {
    val tasks by remember(listId) { viewModel.listTasks(listId) }.collectAsStateWithLifecycle(null)
    val done by remember(listId) { viewModel.completedInList(listId) }.collectAsStateWithLifecycle(emptyList())
    var showDone by rememberSaveable(listId) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val reorder = remember(listState) { ReorderState(listState) }
    val current = tasks ?: return
    LaunchedEffect(current) { reorder.sync(current.map { it.id }) }
    if (current.isEmpty() && done.isEmpty()) {
        EmptyState(
            stringResource(R.string.list_empty_title),
            stringResource(R.string.list_empty_body),
            padding,
            icon = AppIcons.Subtasks,
        )
        return
    }
    PinnedCompletedPage(padding, done.size, pinned = !showDone, onOpen = { showDone = true }) { inner ->
        if (current.isEmpty() && !showDone) {
            EmptyState(stringResource(R.string.list_done_title), stringResource(R.string.list_done_body), inner)
            return@PinnedCompletedPage
        }
        val byId = current.associateBy { it.id }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = inner) {
            items(reorder.order.mapNotNull { byId[it] }, key = { it.id }) { task ->
                ReorderableItem(task, ctx, reorder, viewModel::reorder)
            }
            if (showDone) completedSection(done, ctx, ListLabel.HIDDEN) { showDone = false }
        }
    }
}

/** A list's row: it follows the finger while dragged, by the handle that shows only while selecting. */
@Composable
private fun LazyItemScope.ReorderableItem(
    task: Task,
    ctx: RowContext,
    reorder: ReorderState,
    onDrop: (List<String>, List<String>) -> Unit,
) {
    val dragging = reorder.dragging == task.id
    val rowModifier = if (dragging) {
        Modifier.zIndex(1f).graphicsLayer {
            translationY = reorder.offset
            shadowElevation = DRAG_ELEVATION
        }.background(MaterialTheme.tokens.background)
    } else {
        Modifier.animateItem()
    }
    TaskItem(task, ctx, ctx.meta(task, showDate = true, list = ListLabel.HIDDEN), rowModifier) {
        if (ctx.selection.active) {
            Icon(
                AppIcons.DragHandle,
                stringResource(R.string.action_drag),
                tint = MaterialTheme.tokens.ink3,
                modifier = Modifier.size(48.dp).padding(12.dp).dragHandle(reorder, task.id, onDrop),
            )
        }
    }
}

private const val DRAG_ELEVATION = 12f

// Search

@Composable
fun SearchContent(viewModel: TasksViewModel, ctx: RowContext, padding: PaddingValues) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        SearchField(query, viewModel.query::tryEmit, Modifier.focusRequester(focus))
        when {
            // The field above already took the top padding; below, the room includes an open keyboard.
            query.isBlank() -> EmptyState(
                stringResource(R.string.search_hint_title),
                stringResource(R.string.search_hint_body),
                PaddingValues(bottom = padding.calculateBottomPadding()),
                icon = AppIcons.Search,
            )

            results.isEmpty() -> EmptyState(
                stringResource(R.string.search_none_title),
                stringResource(R.string.search_none_body, query),
                PaddingValues(bottom = padding.calculateBottomPadding()),
                icon = AppIcons.Search,
            )

            else -> SearchResults(results, query, ctx, padding)
        }
    }
}

/** The matching tasks, each title with its matches in the add bar's recognised-word colours. */
@Composable
private fun SearchResults(results: List<Task>, query: String, ctx: RowContext, padding: PaddingValues) {
    val tokens = MaterialTheme.tokens
    val highlight = SpanStyle(background = tokens.highlight, color = tokens.onAccentContainer)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())) {
        items(results, key = { it.id }) { task ->
            val meta = ctx.meta(task, showDate = true, list = ListLabel.ALWAYS)
            val title = highlightMatches(task.title, query, highlight)
            TaskItem(task, ctx, meta, Modifier.animateItem(), title = title)
        }
    }
}

/** Where [query] (trimmed, ignoring case) appears in [text], every time, in order. */
internal fun matchRanges(text: String, query: String): List<IntRange> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    val found = mutableListOf<IntRange>()
    var from = text.indexOf(needle, ignoreCase = true)
    while (from >= 0) {
        found += from until from + needle.length
        from = text.indexOf(needle, from + needle.length, ignoreCase = true)
    }
    return found
}

/** A search result's title with each match set in [highlight], like a recognised word in the add bar. */
internal fun highlightMatches(text: String, query: String, highlight: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        var at = 0
        matchRanges(text, query).forEach { range ->
            append(text.substring(at, range.first))
            withStyle(highlight) { append(text.substring(range)) }
            at = range.last + 1
        }
        append(text.substring(at))
    }

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(R.string.search_placeholder), color = scheme.onSurfaceVariant)
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onBackground),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    AppIcons.Close,
                    stringResource(R.string.action_clear),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") },
                )
            }
        }
    }
}
