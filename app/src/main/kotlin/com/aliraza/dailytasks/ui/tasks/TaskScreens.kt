package com.aliraza.dailytasks.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.Subtask
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.model.TaskList
import com.aliraza.dailytasks.data.SubtaskProgress
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.DateTone
import com.aliraza.dailytasks.ui.components.EmptyState
import com.aliraza.dailytasks.ui.components.RoundCheck
import com.aliraza.dailytasks.ui.components.RowMeta
import com.aliraza.dailytasks.ui.components.SectionHeader
import com.aliraza.dailytasks.ui.components.SubtaskFold
import com.aliraza.dailytasks.ui.components.SwipeRow
import com.aliraza.dailytasks.ui.components.TaskRow
import com.aliraza.dailytasks.ui.components.dueTone
import com.aliraza.dailytasks.ui.components.guarded
import com.aliraza.dailytasks.ui.components.rememberGuardedField
import com.aliraza.dailytasks.ui.components.rowDivider
import com.aliraza.dailytasks.ui.components.sectionDay
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val addSubtask: (taskId: String, title: String) -> Unit = { _, _ -> },
    val delete: (Task) -> Unit = {},
    val reschedule: (List<String>) -> Unit = {},
) {
    val today: LocalDate get() = now.toLocalDate()

    /** List names are shown only for tasks outside the default list, so most rows stay one quiet line. */
    fun meta(task: Task, showDate: Boolean, showList: Boolean = true): RowMeta {
        val list = lists[task.listId]
        return RowMeta(
            date = task.dueDate?.takeIf { showDate },
            time = task.dueTime,
            listName = list?.name?.takeIf { showList && !list.isDefault },
            tone = task.dueTone(now) ?: DateTone.LATER,
            subtasks = subtasks[task.id]?.let { it.done to it.total },
        )
    }
}

@Composable
fun rememberRowContext(
    viewModel: TasksViewModel,
    selection: Selection,
    onReschedule: (List<String>) -> Unit,
    open: (String) -> Unit,
): RowContext {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val progress by viewModel.subtaskProgress.collectAsStateWithLifecycle()
    return remember(now, lists, selection, open, progress, onReschedule) {
        RowContext(
            now,
            lists.associateBy { it.id },
            selection,
            open,
            toggle = { task -> viewModel.setDone(listOf(task.id), task.completedAt == null) },
            subtasks = progress,
            subtasksOf = viewModel::observeSubtasks,
            setSubtaskDone = viewModel::setSubtaskDone,
            addSubtask = viewModel::addSubtask,
            delete = { task -> viewModel.delete(listOf(task.id)) },
            reschedule = onReschedule,
        )
    }
}

/** A task row wired to the screen's selection and sheet, with its sub-tasks folding open in place. */
@Composable
internal fun TaskItem(
    task: Task,
    ctx: RowContext,
    meta: RowMeta,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    // Sub-tasks show by default; the chevron folds them away.
    var open by rememberSaveable(task.id) { mutableStateOf(true) }
    val fold = meta.subtasks?.let { SubtaskFold(open, onToggle = { open = !open }) { InlineSubtasks(task.id, ctx) } }
    SwipeRow(
        enabled = !ctx.selection.active,
        canReschedule = task.completedAt == null,
        onReschedule = { ctx.reschedule(listOf(task.id)) },
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
            fold = fold,
            trailing = trailing,
        )
    }
}

/** The sub-tasks under an open row: tap one, box or text, to tick it. */
@Composable
private fun InlineSubtasks(taskId: String, ctx: RowContext) {
    val subtasks by remember(taskId) { ctx.subtasksOf(taskId) }.collectAsStateWithLifecycle(emptyList())
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 2.dp)) {
        subtasks.forEach { sub ->
            Row(
                // Pulled left so the small boxes line up under the title's first letter.
                Modifier.offset(x = (-12).dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .clickable { ctx.setSubtaskDone(sub, !sub.done) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundCheck(
                    checked = sub.done,
                    onClick = { ctx.setSubtaskDone(sub, !sub.done) },
                    shape = RoundedCornerShape(4.dp),
                    size = 16,
                )
                Text(
                    sub.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sub.done) scheme.onSurfaceVariant else scheme.onBackground,
                    textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AddSubtaskLine { ctx.addSubtask(taskId, it) }
    }
}

/**
 * "+ Add a sub-task" under the open sub-tasks. Tapped, it becomes a field; Enter adds and keeps the
 * field ready for the next one; leaving the field closes it.
 */
@Composable
internal fun AddSubtaskLine(onAdd: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Row(
        Modifier.offset(x = (-12).dp).fillMaxWidth().heightIn(min = 34.dp).clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !editing) { editing = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Quiet on purpose: it sits under every open list of sub-tasks, so it shouldn't compete with them.
        val tint = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(AppIcons.Add, null, tint = tint, modifier = Modifier.padding(start = 13.dp, end = 13.dp).size(14.dp))
        if (editing) SubtaskField(onAdd, onClose = { editing = false }) else AddSubtaskHint()
    }
}

@Composable
private fun AddSubtaskHint() {
    Text(
        stringResource(R.string.subtask_add_inline),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Takes focus when shown; what's typed is added on Enter and when focus leaves, then [onClose]. */
@Composable
private fun RowScope.SubtaskField(onAdd: (String) -> Unit, onClose: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val guarded = rememberGuardedField()
    val scheme = MaterialTheme.colorScheme
    val commit = {
        if (draft.isNotBlank()) onAdd(draft.trim())
        draft = ""
    }
    LaunchedEffect(Unit) { requester.requestFocus() }
    BasicTextField(
        value = draft,
        onValueChange = { draft = it.replace('\n', ' ') },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onBackground),
        cursorBrush = SolidColor(scheme.primary),
        // The hint stays while the field is empty, so an open line still says what it is for.
        decorationBox = { field ->
            Box {
                if (draft.isEmpty()) AddSubtaskHint()
                field()
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = Modifier.weight(1f).guarded(guarded).focusRequester(requester).onFocusChanged {
            if (hadFocus && !it.isFocused) {
                commit()
                onClose()
            }
            hadFocus = it.isFocused
        },
    )
}

private fun LazyListScope.section(
    title: Int?,
    tasks: List<Task>,
    ctx: RowContext,
    showDate: Boolean = false,
    action: Pair<String, () -> Unit>? = null,
) {
    if (tasks.isEmpty()) return
    if (title != null) {
        item(key = "h$title") {
            SectionHeader(
                stringResource(title),
                Modifier.animateItem(),
                trailing = tasks.size.toString(),
                action = action,
            )
        }
    }
    items(tasks, key = { it.id }) { task -> TaskItem(task, ctx, ctx.meta(task, showDate), Modifier.animateItem()) }
}

// Today

/** Share of the screen the "all done" message takes above the completed tasks. */
private const val ALL_DONE_HEIGHT = 0.55f

@Composable
fun TodayContent(viewModel: TasksViewModel, ctx: RowContext, padding: PaddingValues) {
    val sections by viewModel.todaySections.collectAsStateWithLifecycle()
    val done by viewModel.completedToday.collectAsStateWithLifecycle()
    val current = sections ?: return
    var showDone by rememberSaveable { mutableStateOf(false) }
    if (current.all.isEmpty() && done.isEmpty()) {
        EmptyState(
            stringResource(R.string.today_empty_title),
            stringResource(R.string.today_empty_body),
            Modifier.padding(padding),
        )
        return
    }
    // Timed tasks first, then the rest, as one group. It gets a header only to set it apart from overdue.
    val due = current.timed + current.rest
    val overdueIds = current.overdue.map { it.id }
    val moveAll = stringResource(R.string.today_reschedule_overdue) to { ctx.reschedule(overdueIds) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        // Everything for today ticked off: say so above what was done, rather than leave a bare header.
        if (current.all.isEmpty()) {
            item(key = "allDone") {
                EmptyState(
                    stringResource(R.string.today_done_title),
                    stringResource(R.string.today_done_body),
                    Modifier.fillParentMaxHeight(ALL_DONE_HEIGHT),
                )
            }
        }
        section(R.string.today_section_overdue, current.overdue, ctx, showDate = true, action = moveAll)
        section(R.string.day_today.takeIf { current.overdue.isNotEmpty() }, due, ctx)
        completedSection(done, ctx, showDone) { showDone = !showDone }
    }
}

// Upcoming

/** Upcoming: the week strip over one list of days, each picked day jumped to in the list. */
@Composable
fun UpcomingContent(viewModel: TasksViewModel, ctx: RowContext, state: UpcomingState, padding: PaddingValues) {
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val byDay = upcoming ?: return
    val days = state.days(byDay.keys)
    val list = rememberLazyListState()
    val count = { day: LocalDate -> byDay[day].orEmpty().size }
    val jump = rememberDayJump(list) { headerIndex(days, count, it).coerceAtLeast(0) }
    FollowTopDay(list, jump) { dayAt(days, count, it)?.let(state::follow) }
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        val actions = WeekStripActions(
            onHome = {
                state.home()
                jump(state.selected)
            },
            onPage = { forward -> state.page(forward)?.let(jump) },
            onPick = { day ->
                state.selected = day
                jump(day)
            },
        )
        WeekStrip(state, ctx.today, hasTasks = { count(it) > 0 }, actions)
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
        ) {
            days.forEach { day ->
                upcomingDay(day, byDay[day].orEmpty(), ctx, day == state.selected) { state.follow(day) }
            }
        }
    }
}

/**
 * Scrolls the list to a day's header. While it scrolls, [DayJump.running] is set so the strip doesn't
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

/** A day's header: "Tomorrow · Thu 19 Nov". Tapping it picks the day for the add bar. */
private fun LazyListScope.upcomingDay(
    day: LocalDate,
    tasks: List<Task>,
    ctx: RowContext,
    picked: Boolean,
    onPick: () -> Unit,
) {
    item(key = "d$day") {
        SectionHeader(
            sectionDay(day, ctx.today),
            Modifier.clickable(onClick = onPick),
            accent = picked,
            trailing = tasks.size.takeIf { it > 0 }?.toString(),
        )
    }
    items(
        tasks,
        key = { it.id },
    ) { task -> TaskItem(task, ctx, ctx.meta(task, showDate = false), Modifier.animateItem()) }
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
            .rowDivider(MaterialTheme.tokens.divider)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Lists, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(list.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (list.isDefault) {
            Text(
                stringResource(R.string.list_default_tag),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        if (open > 0) {
            Text(open.toString(), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
        Icon(AppIcons.ChevronRight, null, tint = scheme.outline, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun NewListField(onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 18.dp, end = 12.dp),
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

@Composable
fun ListContent(viewModel: TasksViewModel, listId: String, ctx: RowContext, padding: PaddingValues) {
    val tasks by remember(listId) { viewModel.listTasks(listId) }.collectAsStateWithLifecycle(null)
    val done by remember(listId) { viewModel.completedInList(listId) }.collectAsStateWithLifecycle(emptyList())
    var showDone by rememberSaveable(listId) { mutableStateOf(false) }
    val current = tasks ?: return
    if (current.isEmpty() && done.isEmpty()) {
        EmptyState(
            stringResource(R.string.list_empty_title),
            stringResource(R.string.list_empty_body),
            Modifier.padding(padding),
            icon = AppIcons.Checklist,
        )
        return
    }
    val listState = rememberLazyListState()
    val reorder = remember(listState) { ReorderState(listState) }
    LaunchedEffect(current) { reorder.sync(current.map { it.id }) }
    val byId = current.associateBy { it.id }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
        items(reorder.order.mapNotNull { byId[it] }, key = { it.id }) { task ->
            val dragging = reorder.dragging == task.id
            val rowModifier = if (dragging) {
                Modifier.zIndex(1f).graphicsLayer {
                    translationY = reorder.offset
                    shadowElevation = 12f
                }.background(MaterialTheme.colorScheme.surface)
            } else {
                Modifier.animateItem()
            }
            TaskItem(task, ctx, ctx.meta(task, showDate = true, showList = false), rowModifier) {
                Icon(
                    AppIcons.DragHandle,
                    stringResource(R.string.action_drag),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(40.dp).padding(8.dp).dragHandle(reorder, task.id, viewModel::reorder),
                )
            }
        }
        completedSection(done, ctx, showDone, inList = true) { showDone = !showDone }
    }
}

/**
 * Done tasks at the bottom, folded into one "Completed · 3" row. Opened, they show crossed out; ticking
 * one again reopens it and it goes back up.
 */
private fun LazyListScope.completedSection(
    done: List<Task>,
    ctx: RowContext,
    open: Boolean,
    inList: Boolean = false,
    onToggle: () -> Unit,
) {
    if (done.isEmpty()) return
    item(key = "completed") { CompletedHeader(done.size, open, onToggle, Modifier.animateItem()) }
    if (open) {
        items(done, key = { it.id }) { task ->
            TaskItem(task, ctx, ctx.meta(task, showDate = true, showList = !inList), Modifier.animateItem())
        }
    }
}

/** "Completed · 5" with a chevron: a quiet line, no hairline, so done tasks don't compete with open ones. */
@Composable
private fun CompletedHeader(count: Int, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val turn by animateFloatAsState(if (open) HALF_TURN else 0f, label = "chevron")
    Row(
        modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.completed_toggle), onClick = onToggle)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.completed_title, count),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            AppIcons.ChevronDown,
            null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

private const val HALF_TURN = 180f

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
            query.isBlank() -> EmptyState(
                stringResource(R.string.search_hint_title),
                stringResource(R.string.search_hint_body),
                icon = AppIcons.Search,
            )

            results.isEmpty() -> EmptyState(
                stringResource(R.string.search_none_title),
                stringResource(R.string.search_none_body, query),
                icon = AppIcons.Search,
            )

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
            ) {
                items(
                    results,
                    key = { it.id },
                ) { task -> TaskItem(task, ctx, ctx.meta(task, showDate = true), Modifier.animateItem()) }
            }
        }
    }
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

private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
