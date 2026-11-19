package com.aliraza.dailytodos.ui.tasks

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.TaskFocusTotal
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.DueDateSheet
import com.aliraza.dailytodos.ui.components.RoundCheck
import com.aliraza.dailytodos.ui.components.dayLabel
import com.aliraza.dailytodos.ui.components.dueTone
import com.aliraza.dailytodos.ui.components.priorityColor
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.components.toneColor
import com.aliraza.dailytodos.ui.focus.focusTotal
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val SAVE_DELAY_MS = 400L

private const val DEFAULT_PICK_HOUR = 9

/** Everything about one task, in a bottom sheet. Text edits save as you type, after a short pause. */
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
    val subtasks by remember(taskId) { viewModel.observeSubtasks(taskId) }.collectAsStateWithLifecycle(emptyList())
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        val current = task
        if (current == null || current.deletedAt != null) {
            LaunchedEffect(current) { if (current?.deletedAt != null) onDismiss() }
            Spacer(Modifier.height(200.dp))
            return@ModalBottomSheet
        }
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
            SheetHeader(
                lists = lists,
                task = current,
                onList = { id -> viewModel.update(current.id) { it.copy(listId = id) } },
                onDelete = {
                    viewModel.delete(listOf(current.id))
                    onDismiss()
                },
            )
            TextEdits(
                current,
                onSave = { id, title, notes -> viewModel.update(id) { it.copy(title = title, notes = notes) } },
                onToggle = { viewModel.setDone(listOf(current.id), current.completedAt == null) },
            )
            Hairline()
            Properties(viewModel, current, now)
            FocusRow(focused, canStart = current.completedAt == null, onFocus)
            Hairline()
            Subtasks(viewModel, current.id, subtasks)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Hairline() {
    HorizontalDivider(Modifier.padding(vertical = 6.dp), thickness = 0.5.dp, color = MaterialTheme.tokens.divider)
}

/** The list the task sits in (tap to move it), then Focus and Delete. */
@Composable
private fun SheetHeader(lists: List<TaskList>, task: Task, onList: (String) -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.clip(
                    RoundedCornerShape(8.dp),
                ).clickable { open = true }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Lists, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    lists.firstOrNull { it.id == task.listId }?.name.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                Icon(AppIcons.ChevronDown, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                lists.forEach { list ->
                    DropdownMenuItem(text = { Text(list.name) }, onClick = {
                        onList(list.id)
                        open = false
                    })
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(AppIcons.Delete, stringResource(R.string.action_delete), tint = scheme.onSurfaceVariant)
        }
    }
}

/**
 * The title and notes, saved after a pause and when the sheet closes. Each task gets its own editor, so
 * when the sheet moves to another task the typed text is saved to the task it was typed into.
 */
@Composable
internal fun TextEdits(task: Task, onSave: (id: String, title: String, notes: String) -> Unit, onToggle: () -> Unit) {
    key(task.id) { TaskTextEdits(task, onSave, onToggle) }
}

@Composable
private fun TaskTextEdits(
    task: Task,
    onSave: (id: String, title: String, notes: String) -> Unit,
    onToggle: () -> Unit,
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
    val done = task.completedAt != null
    Row(Modifier.padding(start = 6.dp, end = 20.dp, bottom = 8.dp)) {
        RoundCheck(
            checked = done,
            onClick = onToggle,
            priority = task.priority,
            size = 24,
        )
        Column(Modifier.weight(1f).padding(start = 6.dp, top = 6.dp)) {
            PlainField(
                value = title,
                onChange = { title = it.replace('\n', ' ') },
                placeholder = stringResource(R.string.task_title_hint),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                singleLine = false,
            )
            Spacer(Modifier.height(6.dp))
            PlainField(
                value = notes,
                onChange = { notes = it },
                placeholder = stringResource(R.string.task_notes_hint),
                style = MaterialTheme.typography.bodyMedium,
                singleLine = false,
            )
        }
    }
}

@Composable
private fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    singleLine: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box {
        if (value.isEmpty()) Text(placeholder, style = style, color = scheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = style.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Date, time and priority rows, plus the pickers they open. */
@Composable
private fun Properties(viewModel: TasksViewModel, task: Task, now: LocalDateTime) {
    val today = now.toLocalDate()
    var dialog by remember { mutableStateOf<String?>(null) }
    val set = { change: (Task) -> Task -> viewModel.update(task.id, change) }
    val ink = task.dueTone(now)?.let { toneColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    DateRow(task, today, ink) { dialog = "date" }
    TimeRow(task, ink, set) { dialog = "time" }
    PriorityRow(task, set)
    when (dialog) {
        "date" -> DueDateSheet(
            title = stringResource(R.string.due_date_title),
            current = task.dueDate,
            today = today,
            onPick = { date -> set { it.movedTo(date) } },
            onDismiss = { dialog = null },
        )

        "time" -> TimeDialog(task.dueTime ?: LocalTime.of(DEFAULT_PICK_HOUR, 0), onDismiss = { dialog = null }) {
            set { task -> task.copy(dueTime = it, dueDate = task.dueDate ?: today) }
        }
    }
}

/** The due day; tapping it opens the due date sheet. */
@Composable
private fun DateRow(task: Task, today: LocalDate, ink: Color, onOpen: () -> Unit) {
    PropertyRow(
        icon = AppIcons.Today,
        label = task.dueDate?.let { dayLabel(it, today) } ?: stringResource(R.string.task_no_date),
        ink = ink,
        iconTint = ink,
        onClick = onOpen,
    )
}

@Composable
private fun TimeRow(task: Task, ink: Color, set: ((Task) -> Task) -> Unit, onPick: () -> Unit) {
    MenuRow(
        icon = AppIcons.Clock,
        label = task.dueTime?.let { timeLabel(it) } ?: stringResource(R.string.task_no_time),
        ink = if (task.dueTime != null) ink else MaterialTheme.colorScheme.onSurfaceVariant,
    ) { close ->
        Option(R.string.time_pick, close, onPick)
        if (task.dueTime != null) Option(R.string.task_no_time, close) { set { it.copy(dueTime = null) } }
    }
}

@Composable
private fun PriorityRow(task: Task, set: ((Task) -> Task) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    MenuRow(
        icon = AppIcons.Flag,
        label = priorityLabel(task.priority),
        ink = if (task.priority == Priority.NONE) scheme.onSurfaceVariant else scheme.onSurface,
        iconTint = priorityColor(task.priority),
    ) { close ->
        Priority.entries.forEach { p ->
            DropdownMenuItem(
                text = { Text(priorityLabel(p)) },
                leadingIcon = { Icon(AppIcons.Flag, null, tint = priorityColor(p)) },
                onClick = {
                    set { it.copy(priority = p) }
                    close()
                },
            )
        }
    }
}

@Composable
private fun priorityLabel(priority: Priority): String = if (priority == Priority.NONE) {
    stringResource(R.string.priority_none)
} else {
    stringResource(R.string.priority_long, priority.level)
}

@Composable
private fun Option(label: Int, close: () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
        onClick()
        close()
    })
}

/** The focus spent on the task so far; tapping it opens the task's timer. */
@Composable
private fun FocusRow(focused: TaskFocusTotal, canStart: Boolean, onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = focusTotal(focused)
    // A done task keeps its total; there is nothing left to start.
    if (!canStart && focused.sessions == 0) return
    Row(
        Modifier.fillMaxWidth().clickable(enabled = canStart, onClick = onStart).heightIn(min = 48.dp)
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Timer, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (canStart) {
            Icon(
                AppIcons.Play,
                stringResource(R.string.focus_start),
                tint = MaterialTheme.tokens.accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** One property of the task: an icon and its value, as a row to tap. */
@Composable
private fun PropertyRow(icon: ImageVector, label: String, ink: Color, iconTint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ink)
    }
}

/** A property row whose tap opens a menu of choices. */
@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    ink: Color,
    iconTint: Color = ink,
    menu: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        PropertyRow(icon, label, ink, iconTint) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            menu { open = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state) },
    )
}

@Composable
private fun Subtasks(viewModel: TasksViewModel, taskId: String, subtasks: List<Subtask>) {
    val scheme = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.task_subtasks),
        style = MaterialTheme.typography.titleSmall,
        color = scheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp),
    )
    subtasks.forEach { sub ->
        Row(Modifier.padding(start = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundCheck(
                checked = sub.done,
                onClick = { viewModel.setSubtaskDone(sub, !sub.done) },
                shape = RoundedCornerShape(5.dp),
                size = 18,
            )
            Text(
                sub.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (sub.done) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(
                AppIcons.Close,
                stringResource(R.string.action_remove),
                tint = scheme.outline,
                modifier = Modifier.size(
                    36.dp,
                ).clip(CircleShape).clickable { viewModel.deleteSubtask(sub.id) }.padding(9.dp),
            )
        }
    }
    var draft by rememberSaveable(taskId) { mutableStateOf("") }
    Row(Modifier.padding(start = 20.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Add, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (draft.isEmpty()) {
                Text(
                    stringResource(R.string.task_subtask_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    viewModel.addSubtask(taskId, draft)
                    draft = ""
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
