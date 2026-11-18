package com.aliraza.dailytasks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.MainActivity
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.TaskList
import com.aliraza.dailytasks.ui.calendar.CalendarContent
import com.aliraza.dailytasks.ui.calendar.CalendarMode
import com.aliraza.dailytasks.ui.calendar.CalendarState
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.KeyboardGuardBox
import com.aliraza.dailytasks.ui.components.QuickAddBar
import com.aliraza.dailytasks.ui.components.QuickAddState
import com.aliraza.dailytasks.ui.components.due
import com.aliraza.dailytasks.ui.components.longDateLabel
import com.aliraza.dailytasks.ui.components.switchedOffWhen
import com.aliraza.dailytasks.ui.focus.FocusScreen
import com.aliraza.dailytasks.ui.focus.FocusViewModel
import com.aliraza.dailytasks.ui.settings.HealthScreen
import com.aliraza.dailytasks.ui.settings.SettingsScreen
import com.aliraza.dailytasks.ui.settings.SettingsViewModel
import com.aliraza.dailytasks.ui.tasks.ListContent
import com.aliraza.dailytasks.ui.tasks.ListsContent
import com.aliraza.dailytasks.ui.tasks.RescheduleHost
import com.aliraza.dailytasks.ui.tasks.RowContext
import com.aliraza.dailytasks.ui.tasks.SearchContent
import com.aliraza.dailytasks.ui.tasks.Selection
import com.aliraza.dailytasks.ui.tasks.TaskSheet
import com.aliraza.dailytasks.ui.tasks.TasksViewModel
import com.aliraza.dailytasks.ui.tasks.TodayContent
import com.aliraza.dailytasks.ui.tasks.UpcomingContent
import com.aliraza.dailytasks.ui.tasks.UpcomingState
import com.aliraza.dailytasks.ui.tasks.rememberRowContext
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

/** Where the app is. A plain back stack of these; the first four are the bottom tabs. */
private object Route {
    const val TODAY = "today"
    const val UPCOMING = "upcoming"
    const val CALENDAR = "calendar"
    const val LISTS = "lists"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val HEALTH = "health"
    const val FOCUS = "focus"
    const val LIST_PREFIX = "list/"

    val tabs = listOf(TODAY, UPCOMING, CALENDAR, LISTS)
}

private fun String.listId(): String? = removePrefix(Route.LIST_PREFIX).takeIf { startsWith(Route.LIST_PREFIX) }

@Composable
fun AppRoot(
    tasks: TasksViewModel,
    settings: SettingsViewModel,
    focus: FocusViewModel,
    opened: MainActivity.Opened?,
    onOpenedHandled: () -> Unit,
) {
    var stack by rememberSaveable { mutableStateOf(listOf(Route.TODAY)) }
    var sheetTask by rememberSaveable { mutableStateOf<String?>(null) }
    var rescheduling by rememberSaveable { mutableStateOf<String?>(null) }
    val route = stack.last()
    val navigate = { next: String -> stack = stack + next }
    val back = { stack = stack.dropLast(1) }
    val selection = remember { Selection() }
    val upcoming = remember { UpcomingState(tasks.now.value.toLocalDate()) }
    val calendar = remember { CalendarState(tasks.now.value.toLocalDate()) }
    LaunchedEffect(route) { selection.clear() }
    OpenedEffect(
        opened,
        onTask = { sheetTask = it },
        onFocus = { stack = stack.filter { it != Route.FOCUS } + Route.FOCUS },
        onHandled = onOpenedHandled,
    )
    BackHandler(enabled = selection.active || stack.size > 1) { if (selection.active) selection.clear() else back() }

    val snackbar = remember { SnackbarHostState() }
    UndoSnackbars(tasks, snackbar)
    val ctx = rememberRowContext(tasks, selection, onReschedule = { rescheduling = it }) { sheetTask = it }
    val quickAdd = remember { QuickAddState() }
    val showsAdd = route in setOf(Route.TODAY, Route.UPCOMING, Route.CALENDAR) || route.listId() != null
    val canAdd = showsAdd && !selection.active

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    data,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
        topBar = {
            if (selection.active) {
                SelectionBar(selection, tasks)
            } else {
                TopBar(route, tasks, onBack = back, onOpen = navigate)
            }
        },
        floatingActionButton = {
            if (canAdd && !quickAdd.open) AddButton(quickAdd::show)
        },
        bottomBar = {
            BottomBars(route, showAdd = canAdd && quickAdd.open, onTab = { stack = listOf(it) }) {
                AddBar(tasks, quickAdd, route, contextDate(route, upcoming, calendar))
            }
        },
    ) { padding ->
        val screens = Screens(tasks, settings, focus, ctx, upcoming, calendar, navigate) { day ->
            calendar.open(day, CalendarMode.WEEK)
            stack = listOf(Route.CALENDAR)
        }
        Content(route, screens, padding, addButton = showsAdd, typing = quickAdd.focused)
    }
    rescheduling?.let { id -> RescheduleHost(tasks, id) { rescheduling = null } }
    sheetTask?.let { id ->
        TaskSheet(
            viewModel = tasks,
            taskId = id,
            onFocus = {
                focus.pickTask(it)
                sheetTask = null
                navigate(Route.FOCUS)
            },
            onDismiss = { sheetTask = null },
        )
    }
}

/**
 * The current screen, faded between routes. With an [addButton] the last row keeps clear of it; while
 * [typing] a tap anywhere only closes the keyboard.
 */
@Composable
private fun Content(route: String, screens: Screens, padding: PaddingValues, addButton: Boolean, typing: Boolean) {
    val inner = if (addButton) padding.clearOfAddButton() else padding
    KeyboardGuardBox(alsoWhen = typing) {
        AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route") { shown ->
            RouteContent(shown, screens, inner)
        }
    }
}

/** Acts on what a notification opened: a task's sheet, or the focus timer. */
@Composable
private fun OpenedEffect(
    opened: MainActivity.Opened?,
    onTask: (String) -> Unit,
    onFocus: () -> Unit,
    onHandled: () -> Unit,
) {
    LaunchedEffect(opened) {
        when (opened) {
            is MainActivity.Opened.Task -> onTask(opened.id)
            MainActivity.Opened.Focus -> onFocus()
            null -> return@LaunchedEffect
        }
        onHandled()
    }
}

/** The round + that opens the add bar. */
@Composable
private fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.tokens.accent,
        contentColor = MaterialTheme.tokens.onAccent,
    ) {
        Icon(AppIcons.Add, stringResource(R.string.add_placeholder), modifier = Modifier.size(28.dp))
    }
}

/** Room under the last row, so the + button never covers it. */
@Composable
private fun PaddingValues.clearOfAddButton(): PaddingValues = PaddingValues(
    top = calculateTopPadding(),
    bottom = calculateBottomPadding() + AddButtonClearance,
)

private val AddButtonClearance = 80.dp

/** What the screens need from the shell. */
private class Screens(
    val tasks: TasksViewModel,
    val settings: SettingsViewModel,
    val focus: FocusViewModel,
    val ctx: RowContext,
    val upcoming: UpcomingState,
    val calendar: CalendarState,
    val navigate: (String) -> Unit,
    val openCalendar: (LocalDate) -> Unit,
)

/** The day the add bar files new tasks under: the picked day in Upcoming or the calendar. */
private fun contextDate(route: String, upcoming: UpcomingState, calendar: CalendarState): LocalDate? = when (route) {
    Route.UPCOMING -> upcoming.selected
    Route.CALENDAR -> calendar.anchor
    else -> null
}

@Composable
private fun RouteContent(route: String, s: Screens, padding: PaddingValues) {
    val list = route.listId()
    when {
        list != null -> ListContent(s.tasks, list, s.ctx, padding)
        route == Route.TODAY -> TodayContent(s.tasks, s.ctx, padding)
        route == Route.UPCOMING -> UpcomingContent(s.tasks, s.ctx, s.upcoming, padding, s.openCalendar)
        route == Route.CALENDAR -> CalendarContent(s.tasks, s.ctx, s.calendar, padding)
        route == Route.LISTS -> ListsContent(s.tasks, padding) { s.navigate(Route.LIST_PREFIX + it) }
        route == Route.SEARCH -> SearchContent(s.tasks, s.ctx, padding)
        route == Route.SETTINGS -> SettingsScreen(s.settings, padding) { s.navigate(Route.HEALTH) }
        route == Route.HEALTH -> HealthScreen(s.settings, padding)
        route == Route.FOCUS -> FocusScreen(s.focus, padding)
    }
}

/** The add bar above the tabs. While typing the tabs step aside, so the bar sits on the keyboard. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomBars(route: String, showAdd: Boolean, onTab: (String) -> Unit, addBar: @Composable () -> Unit) {
    val typing = WindowInsets.isImeVisible
    val focus = LocalFocusManager.current
    // Closing the keyboard ends the edit, so no cursor is left blinking in the bar.
    LaunchedEffect(typing) { if (!typing) focus.clearFocus() }
    Column(if (typing) Modifier.imePadding() else Modifier) {
        if (showAdd) addBar()
        if (route in Route.tabs && !typing) {
            TabBar(route, onTab)
        } else if (showAdd) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun UndoSnackbars(tasks: TasksViewModel, host: SnackbarHostState) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(tasks) {
        tasks.undo.collectLatest { event ->
            val message = resources.getQuantityString(event.message, event.count, event.count)
            val result = host.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) event.undo()
        }
    }
}

@Composable
private fun AddBar(tasks: TasksViewModel, state: QuickAddState, route: String, contextDate: LocalDate?) {
    val now by tasks.now.collectAsStateWithLifecycle()
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val settings by tasks.settings.collectAsStateWithLifecycle()
    val listId = route.listId()
    val parsed = remember(state.text, state.excluded, lists, now) { tasks.parse(state.text, state.excluded.keys) }
    val all = remember(state.text, lists, now) { tasks.parse(state.text, emptySet()) }
    // A typed date switched off means "no date", so the default stays out while one is.
    val defaultOff = state.defaultDateOff || switchedOffWhen(all, state.excluded.keys).isNotEmpty()
    val preview = tasks.defaultDate(parsed, settings, listId, contextDate, off = false)
    val placeholder = lists.firstOrNull { it.id == listId }?.let { stringResource(R.string.add_to_list, it.name) }
        ?: stringResource(R.string.add_placeholder)
    QuickAddBar(
        state = state,
        parsed = parsed,
        due = due(parsed, all, state.excluded.keys, preview, defaultOff),
        today = now.toLocalDate(),
        placeholder = placeholder,
        onSubmit = {
            // Parsed again from the field itself: a second Enter before recomposition must not add twice.
            if (state.text.isNotBlank()) {
                val line = tasks.parse(state.text, state.excluded.keys)
                val off = state.defaultDateOff ||
                    switchedOffWhen(tasks.parse(state.text, emptySet()), state.excluded.keys).isNotEmpty()
                tasks.add(line, listId, tasks.defaultDate(line, settings, listId, contextDate, off))
                state.clear()
            }
        },
    )
}

@Composable
private fun TopBar(route: String, tasks: TasksViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val now by tasks.now.collectAsStateWithLifecycle()
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val list = route.listId()?.let { id -> lists.firstOrNull { it.id == id } }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val title = list?.name ?: stringResource(titleOf(route))
    val top = route in Route.tabs
    // Opaque up to the screen's top edge, so rows scrolled up pass under it instead of showing through.
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(
            start = if (top) 16.dp else 4.dp,
            end = 4.dp,
            top = 8.dp,
            bottom = 4.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!top) {
            IconButton(onClick = onBack) { Icon(AppIcons.Back, stringResource(R.string.action_back)) }
        }
        TitleBlock(
            title = title,
            subtitle = longDateLabel(now.toLocalDate()).takeIf { route == Route.TODAY },
            // A list is a place of its own, so it gets the same large title as the tabs.
            large = top || list != null,
            modifier = Modifier.weight(1f),
        )
        if (list != null) ListMenu(list, onRename = { renaming = true }) { deleting = true }
        if (top) {
            val tint = MaterialTheme.colorScheme.onSurfaceVariant
            IconButton(onClick = { onOpen(Route.SEARCH) }) {
                Icon(AppIcons.Search, stringResource(R.string.search_title), tint = tint)
            }
            IconButton(onClick = { onOpen(Route.FOCUS) }) {
                Icon(AppIcons.Timer, stringResource(R.string.focus_title), tint = tint)
            }
            IconButton(onClick = { onOpen(Route.SETTINGS) }) {
                Icon(AppIcons.Settings, stringResource(R.string.settings_title), tint = tint)
            }
        }
    }
    if (renaming && list != null) {
        RenameDialog(list.name, onDismiss = { renaming = false }) { tasks.renameList(list.id, it) }
    }
    if (deleting && list != null) {
        DeleteListDialog(list.name, onDismiss = { deleting = false }) {
            tasks.deleteList(list.id)
            onBack()
        }
    }
}

/** A list's ⋮ menu: rename, and delete for every list but the default one. */
@Composable
private fun ListMenu(list: TaskList, onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(AppIcons.More, stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_rename)) },
                leadingIcon = { Icon(AppIcons.Edit, null) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            if (!list.isDefault) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.list_delete), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(AppIcons.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        open = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** Asks before a list goes, since its tasks go with it. Undo is still offered right after. */
@Composable
private fun DeleteListDialog(name: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_delete_title, name)) },
        text = { Text(stringResource(R.string.list_delete_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onDelete()
            }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            // Neutral, so the one red button is the one that deletes.
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, large: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(
            title,
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun titleOf(route: String): Int = when (route) {
    Route.TODAY -> R.string.today_title
    Route.UPCOMING -> R.string.upcoming_title
    Route.CALENDAR -> R.string.calendar_title
    Route.LISTS -> R.string.lists_title
    Route.SEARCH -> R.string.search_title
    Route.SETTINGS -> R.string.settings_title
    Route.HEALTH -> R.string.health_title
    else -> R.string.focus_title
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_rename)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onRename(name)
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun SelectionBar(selection: Selection, tasks: TasksViewModel) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(
            scheme.primaryContainer,
        ).statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = selection::clear,
        ) { Icon(AppIcons.Close, stringResource(R.string.action_cancel), tint = scheme.onPrimaryContainer) }
        Text(
            pluralStringResource(R.plurals.selected_count, selection.ids.size, selection.ids.size),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = {
            tasks.setDone(selection.ids, done = true)
            selection.clear()
        }) { Icon(AppIcons.Check, stringResource(R.string.action_complete), tint = scheme.onPrimaryContainer) }
        IconButton(onClick = {
            tasks.delete(selection.ids)
            selection.clear()
        }) { Icon(AppIcons.Delete, stringResource(R.string.action_delete), tint = scheme.onPrimaryContainer) }
    }
}

@Composable
private fun TabBar(route: String, onTab: (String) -> Unit) {
    val items = listOf(
        Triple(Route.TODAY, AppIcons.Today, R.string.today_title),
        Triple(Route.UPCOMING, AppIcons.Upcoming, R.string.upcoming_title),
        Triple(Route.CALENDAR, AppIcons.Calendar, R.string.calendar_title),
        Triple(Route.LISTS, AppIcons.Lists, R.string.lists_title),
    )
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .topHairline(MaterialTheme.tokens.divider).navigationBarsPadding().padding(vertical = 2.dp),
    ) {
        items.forEach { (target, icon, label) ->
            Tab(
                icon,
                stringResource(label),
                selected = route == target,
            ) { onTab(target) }
        }
    }
}

@Composable
private fun RowScope.Tab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (selected) scheme.secondary else scheme.onSurfaceVariant
    Column(
        Modifier.weight(1f).clickable(role = Role.Tab, onClick = onClick).padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = ink,
        )
    }
}

/** A hairline along the top edge. */
private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx() / 2)
}
