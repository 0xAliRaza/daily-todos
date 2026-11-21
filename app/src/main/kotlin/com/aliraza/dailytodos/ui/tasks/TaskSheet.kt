package com.aliraza.dailytodos.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.TaskFocusTotal
import com.aliraza.dailytodos.ui.components.AppButton
import com.aliraza.dailytodos.ui.components.AppButtons
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.AppSheet
import com.aliraza.dailytodos.ui.components.DEFAULT_PICK_HOUR
import com.aliraza.dailytodos.ui.components.DateTone
import com.aliraza.dailytodos.ui.components.DueChoice
import com.aliraza.dailytodos.ui.components.DueDateContent
import com.aliraza.dailytodos.ui.components.RoundCheck
import com.aliraza.dailytodos.ui.components.TimeDialog
import com.aliraza.dailytodos.ui.components.dayLabel
import com.aliraza.dailytodos.ui.components.dueTone
import com.aliraza.dailytodos.ui.components.priorityColor
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.focus.focusTotal
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.groupLabel
import com.aliraza.dailytodos.ui.theme.sheetTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime

private const val SAVE_DELAY_MS = 400L

/**
 * Everything about one task, in a bottom sheet. Text edits save as you type, after a short pause. The
 * date row swaps the sheet's content for the date view, so picking a date never stacks a second layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    viewModel: TasksViewModel,
    taskId: String,
    focused: TaskFocusTotal,
    onFocus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val task by remember(taskId) { viewModel.observeTask(taskId) }.collectAsStateWithLifecycle(null)
    val now by viewModel.now.collectAsStateWithLifecycle()
    var dating by rememberSaveable(taskId) { mutableStateOf(false) }
    AppSheet(onDismiss) {
        val current = task
        if (current == null || current.deletedAt != null) {
            LaunchedEffect(current) { if (current?.deletedAt != null) onDismiss() }
            Spacer(Modifier.height(200.dp))
            return@AppSheet
        }
        if (dating) {
            BackHandler { dating = false }
            DueDateContent(
                title = current.title,
                initial = DueChoice(current.dueDate, current.dueTime),
                today = now.toLocalDate(),
                withTime = true,
                onPick = { choice ->
                    viewModel.update(current.id) { it.copy(dueDate = choice.date, dueTime = choice.time) }
                },
                onClose = { dating = false },
                onBack = { dating = false },
            )
        } else {
            TaskDetails(viewModel, current, now, focused, onFocus, onDate = { dating = true }, onDismiss)
        }
    }
}

/** The task's title, notes, properties, sub-tasks and focus, with Focus and Delete at the bottom. */
@Composable
private fun TaskDetails(
    viewModel: TasksViewModel,
    task: Task,
    now: LocalDateTime,
    focused: TaskFocusTotal,
    onFocus: () -> Unit,
    onDate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val subtasks by remember(task.id) { viewModel.observeSubtasks(task.id) }.collectAsStateWithLifecycle(emptyList())
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val delete = {
        viewModel.delete(listOf(task.id))
        onDismiss()
    }
    val moveTo = { id: String -> viewModel.moveToList(listOf(task.id), id) }
    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
        TextEdits(
            task,
            onSave = { id, title, notes -> viewModel.update(id) { it.copy(title = title, notes = notes) } },
            onToggle = { viewModel.setDone(listOf(task.id), task.completedAt == null) },
            trailing = { TaskMenu(lists, onMove = moveTo, onDelete = delete) },
        )
        Properties(viewModel, task, lists, now, onDate, moveTo)
        Subtasks(viewModel, task.id, subtasks)
        FocusSummary(focused)
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppButton(
                stringResource(R.string.action_focus),
                onFocus,
                AppButtons.primary(),
                Modifier.weight(1f),
                icon = AppIcons.Play,
                enabled = task.completedAt == null,
            )
            AppButton(stringResource(R.string.action_delete), delete, AppButtons.danger(), icon = AppIcons.Delete)
        }
    }
}

/** The ⋮ menu by the title: move the task to another list, or delete it. */
@Composable
private fun TaskMenu(lists: List<TaskList>, onMove: (String) -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(SheetMenu.CLOSED) }
    val close = { menu = SheetMenu.CLOSED }
    Box {
        IconButton(onClick = { menu = SheetMenu.MAIN }) {
            Icon(AppIcons.More, stringResource(R.string.action_more), tint = MaterialTheme.tokens.ink)
        }
        DropdownMenu(expanded = menu == SheetMenu.MAIN, onDismissRequest = close) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sheet_move)) },
                leadingIcon = { Icon(AppIcons.Move, null) },
                onClick = { menu = SheetMenu.LISTS },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.tokens.danger) },
                leadingIcon = { Icon(AppIcons.Delete, null, tint = MaterialTheme.tokens.danger) },
                onClick = {
                    close()
                    onDelete()
                },
            )
        }
        ListMenu(menu == SheetMenu.LISTS, lists, close, onMove)
    }
}

private enum class SheetMenu { CLOSED, MAIN, LISTS }

@Composable
private fun ListMenu(expanded: Boolean, lists: List<TaskList>, onClose: () -> Unit, onPick: (String) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onClose) {
        lists.forEach { list ->
            DropdownMenuItem(text = { Text(list.name) }, onClick = {
                onPick(list.id)
                onClose()
            })
        }
    }
}

/**
 * The title and notes, saved after a pause and when the sheet closes. Each task gets its own editor, so
 * when the sheet moves to another task the typed text is saved to the task it was typed into.
 */
@Composable
internal fun TextEdits(
    task: Task,
    onSave: (id: String, title: String, notes: String) -> Unit,
    onToggle: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    key(task.id) { TaskTextEdits(task, onSave, onToggle, trailing) }
}

@Composable
private fun TaskTextEdits(
    task: Task,
    onSave: (id: String, title: String, notes: String) -> Unit,
    onToggle: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    val latest by rememberUpdatedState(task)
    val save = {
        val t = title.trim()
        if ((t.isNotEmpty() && t != latest.title) || notes != latest.notes) {
            onSave(latest.id, t.ifEmpty { latest.title }, notes)
        }
    }
    LaunchedEffect(title, notes) {
        delay(SAVE_DELAY_MS)
        save()
    }
    DisposableEffect(Unit) { onDispose { save() } }
    Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
        RoundCheck(checked = task.completedAt != null, onClick = onToggle, priority = task.priority)
        Box(Modifier.weight(1f).padding(start = 2.dp, top = 9.dp, bottom = 4.dp)) {
            PlainField(
                value = title,
                onChange = { title = it.replace('\n', ' ') },
                placeholder = stringResource(R.string.task_title_hint),
                style = MaterialTheme.typography.sheetTitle,
            )
        }
        trailing()
    }
    NotesField(notes, onChange = { notes = it })
}

/** The notes in a filled box under the title, so they read as a field to write in. */
@Composable
private fun NotesField(notes: String, onChange: (String) -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp).fillMaxWidth()
            .heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(tokens.fill)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AppIcons.Notes, null, tint = tokens.ink2, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        PlainField(
            value = notes,
            onChange = onChange,
            placeholder = stringResource(R.string.task_notes_hint),
            style = MaterialTheme.typography.body,
        )
    }
}

@Composable
private fun PlainField(value: String, onChange: (String) -> Unit, placeholder: String, style: TextStyle) {
    val tokens = MaterialTheme.tokens
    Box {
        if (value.isEmpty()) Text(placeholder, style = style, color = tokens.ink2)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = style.copy(color = tokens.ink),
            cursorBrush = SolidColor(tokens.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Date, time, priority and list, each a labelled row with its value; the time opens the clock. */
@Composable
private fun Properties(
    viewModel: TasksViewModel,
    task: Task,
    lists: List<TaskList>,
    now: LocalDateTime,
    onDate: () -> Unit,
    onMove: (String) -> Unit,
) {
    val today = now.toLocalDate()
    var clock by remember { mutableStateOf(false) }
    val set = { change: (Task) -> Task -> viewModel.update(task.id, change) }
    val tokens = MaterialTheme.tokens
    // A set value reads in ink, red once overdue; an empty one stays quiet.
    val dueInk = if (task.dueTone(now) == DateTone.OVERDUE) tokens.danger else tokens.ink
    DetailRow(
        AppIcons.Calendar,
        tokens.ink2,
        stringResource(R.string.add_option_date),
        task.dueDate?.let { dayLabel(it, today) } ?: stringResource(R.string.task_no_date),
        if (task.dueDate != null) dueInk else tokens.ink2,
        onDate,
    )
    DetailRow(
        AppIcons.Clock,
        tokens.ink2,
        stringResource(R.string.date_time),
        task.dueTime?.let { timeLabel(it) } ?: stringResource(R.string.task_no_time),
        if (task.dueTime != null) dueInk else tokens.ink2,
    ) { clock = true }
    PriorityRow(task, set)
    var listMenu by remember { mutableStateOf(false) }
    Box {
        DetailRow(
            AppIcons.Lists,
            tokens.ink2,
            stringResource(R.string.sheet_list),
            lists.firstOrNull { it.id == task.listId }?.name.orEmpty(),
            tokens.ink,
        ) { listMenu = true }
        ListMenu(listMenu, lists, onClose = { listMenu = false }, onPick = onMove)
    }
    if (clock) {
        TimeDialog(
            initial = task.dueTime ?: LocalTime.of(DEFAULT_PICK_HOUR, 0),
            onDismiss = { clock = false },
            onPick = { time -> set { it.copy(dueTime = time, dueDate = it.dueDate ?: today) } },
            onClear = { set { it.copy(dueTime = null) } }.takeIf { task.dueTime != null },
        )
    }
}

@Composable
private fun PriorityRow(task: Task, set: ((Task) -> Task) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val none = task.priority == Priority.NONE
    Box {
        DetailRow(
            AppIcons.Flag,
            if (none) MaterialTheme.tokens.ink2 else priorityColor(task.priority),
            stringResource(R.string.add_option_priority),
            if (none) {
                stringResource(R.string.sheet_priority_none)
            } else {
                stringResource(R.string.priority_short, task.priority.level)
            },
            if (none) MaterialTheme.tokens.ink2 else MaterialTheme.tokens.ink,
        ) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Priority.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(priorityLabel(p)) },
                    leadingIcon = { Icon(AppIcons.Flag, null, tint = priorityColor(p)) },
                    onClick = {
                        set { it.copy(priority = p) }
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun priorityLabel(priority: Priority): String = if (priority == Priority.NONE) {
    stringResource(R.string.priority_none)
} else {
    stringResource(R.string.priority_long, priority.level)
}

/** One property: its icon and name at the left, its value and a chevron at the right. */
@Composable
private fun DetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueInk: Color,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().rowDivider(tokens.hairline).clickable(onClick = onClick).heightIn(min = 52.dp)
            .padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.body, color = tokens.ink, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.body,
            fontWeight = FontWeight.Medium,
            color = valueInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(AppIcons.ChevronRight, null, tint = tokens.ink3, modifier = Modifier.size(18.dp))
    }
}

/** "SUB-TASKS · 1/3", each sub-task with its box and ×, then the line that adds one. */
@Composable
private fun Subtasks(viewModel: TasksViewModel, taskId: String, subtasks: List<Subtask>) {
    val tokens = MaterialTheme.tokens
    val header = if (subtasks.isEmpty()) {
        stringResource(R.string.task_subtasks)
    } else {
        stringResource(R.string.sheet_subtasks_count, subtasks.count { it.done }, subtasks.size)
    }
    Text(
        header.uppercase(),
        style = MaterialTheme.typography.groupLabel,
        color = tokens.ink2,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
    )
    subtasks.forEach { sub ->
        Row(
            Modifier.heightIn(min = 44.dp).padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundCheck(checked = sub.done, onClick = { viewModel.setSubtaskDone(sub, !sub.done) }, size = 18.dp)
            Text(
                sub.title,
                style = MaterialTheme.typography.body,
                color = if (sub.done) tokens.ink2 else tokens.ink,
                modifier = Modifier.weight(1f).padding(start = 2.dp),
            )
            Icon(
                AppIcons.Close,
                stringResource(R.string.action_remove),
                tint = tokens.ink3,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { viewModel.deleteSubtask(sub.id) }
                    .padding(9.dp),
            )
        }
    }
    NewSubtask { viewModel.addSubtask(taskId, it) }
}

/** "+ Add a sub-task" in the accent, as a field: Enter adds and keeps it ready for the next one. */
@Composable
private fun NewSubtask(onAdd: (String) -> Unit) {
    val tokens = MaterialTheme.tokens
    var draft by rememberSaveable { mutableStateOf("") }
    val hint = MaterialTheme.typography.button.copy(fontSize = MaterialTheme.typography.body.fontSize)
    Row(
        Modifier.heightIn(min = 44.dp).padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Add, null, tint = tokens.accent, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f)) {
            if (draft.isEmpty()) Text(stringResource(R.string.task_subtask_hint), style = hint, color = tokens.accent)
            BasicTextField(
                value = draft,
                onValueChange = { draft = it.replace('\n', ' ') },
                singleLine = true,
                textStyle = MaterialTheme.typography.body.copy(color = tokens.ink),
                cursorBrush = SolidColor(tokens.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (draft.isNotBlank()) onAdd(draft.trim())
                    draft = ""
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "Focused 1 h 20 min · 4 sessions", or "No focus yet", over a hairline. */
@Composable
private fun FocusSummary(focused: TaskFocusTotal) {
    val tokens = MaterialTheme.tokens
    Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(tokens.hairline))
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AppIcons.Timer, null, tint = tokens.ink2, modifier = Modifier.size(18.dp))
        Text(
            if (focused.sessions == 0) stringResource(R.string.sheet_focus_none) else focusTotal(focused),
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Medium,
            color = tokens.ink2,
        )
    }
}
