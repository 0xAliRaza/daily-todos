package com.aliraza.dailytodos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Transition
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.MainActivity
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.TaskFocusTotal
import com.aliraza.dailytodos.ui.calendar.CalendarContent
import com.aliraza.dailytodos.ui.calendar.CalendarState
import com.aliraza.dailytodos.ui.components.AddOptions
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.KeyboardGuardBox
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.components.QuickAddBar
import com.aliraza.dailytodos.ui.components.QuickAddState
import com.aliraza.dailytodos.ui.components.due
import com.aliraza.dailytodos.ui.components.longDateLabel
import com.aliraza.dailytodos.ui.components.switchedOffWhen
import com.aliraza.dailytodos.ui.focus.FocusScreen
import com.aliraza.dailytodos.ui.focus.FocusViewModel
import com.aliraza.dailytodos.ui.focus.NowFocusingBar
import com.aliraza.dailytodos.ui.settings.FocusStatsScreen
import com.aliraza.dailytodos.ui.settings.HealthScreen
import com.aliraza.dailytodos.ui.settings.SettingsScreen
import com.aliraza.dailytodos.ui.settings.SettingsViewModel
import com.aliraza.dailytodos.ui.tasks.ListContent
import com.aliraza.dailytodos.ui.tasks.ListsContent
import com.aliraza.dailytodos.ui.tasks.RescheduleHost
import com.aliraza.dailytodos.ui.tasks.RowContext
import com.aliraza.dailytodos.ui.tasks.SearchContent
import com.aliraza.dailytodos.ui.tasks.Selection
import com.aliraza.dailytodos.ui.tasks.TaskSheet
import com.aliraza.dailytodos.ui.tasks.TasksViewModel
import com.aliraza.dailytodos.ui.tasks.TodayContent
import com.aliraza.dailytodos.ui.tasks.UpcomingContent
import com.aliraza.dailytodos.ui.tasks.UpcomingState
import com.aliraza.dailytodos.ui.tasks.rememberRowContext
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun AppRoot(
    tasks: TasksViewModel,
    settings: SettingsViewModel,
    focus: FocusViewModel,
    opened: MainActivity.Opened?,
    onOpenedHandled: () -> Unit,
) {
    val nav = rememberNavigation()
    var sheetTask by rememberSaveable { mutableStateOf<String?>(null) }
    var rescheduling by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val route = nav.route
    val selection = remember { Selection() }
    // Read through derivedStateOf so the root recomposes when the date turns, not every minute.
    val clock = tasks.now.collectAsStateWithLifecycle()
    val today by remember { derivedStateOf { clock.value.toLocalDate() } }
    val weekStart = LocalWeekStart.current
    val upcoming = remember(today, weekStart) { UpcomingState(today, weekStart) }
    val calendar = remember { CalendarState(tasks.now.value.toLocalDate()) }
    LaunchedEffect(route) { selection.clear() }
    OpenedEffect(
        opened,
        onTask = { sheetTask = it },
        onFocus = nav::openFocus,
        onHandled = onOpenedHandled,
    )
    // Clearing a selection wins over going back; a back swipe otherwise lets the page below peek in.
    BackHandler(enabled = selection.active) { selection.clear() }
    val tabSeek = remember { TabSeek() }
    val pages = rememberPageTransition(nav, backEnabled = !selection.active, tabSeek)

    val snackbar = remember { SnackbarHostState() }
    UndoSnackbars(tasks, snackbar)
    val ctx = rememberRowContext(
        tasks,
        selection,
        onReschedule = { if (it.isNotEmpty()) rescheduling = ArrayList(it) },
        onFocus = nav::openFocus,
    ) { sheetTask = it }
    val quickAdd = remember { QuickAddState() }
    val canAdd = showsAdd(route) && !selection.active
    val topBar: @Composable (String) -> Unit = { shown ->
        // A page sliding away keeps its title; only the page in front shows the selection.
        RootTopBar(shown, selection.active && shown == route, selection, tasks, nav) {
            if (it.isNotEmpty()) rescheduling = ArrayList(it)
        }
    }

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
        floatingActionButton = {
            if (canAdd && !quickAdd.open) AddButton(quickAdd::show)
        },
        bottomBar = {
            BottomBars(
                route,
                showAdd = canAdd && quickAdd.open,
                picking = quickAdd.picking,
                onTab = nav::tab,
                nowFocusing = nowFocusing(focus, route, nav::openFocus),
            ) {
                AddBar(tasks, quickAdd, route, contextDate(route, upcoming, calendar))
            }
        },
    ) { padding ->
        val screens = Screens(tasks, settings, focus, ctx, upcoming, calendar, nav)
        val onTab = nav::tab.takeIf { !selection.active && !quickAdd.open }
        Pages(pages, screens, topBar, padding, PageInput(quickAdd.focused, quickAdd.open, tabSeek, onTab))
    }
    rescheduling?.let { ids -> RescheduleHost(tasks, ids) { rescheduling = null } }
    sheetTask?.let { id ->
        TaskSheetHost(tasks, focus, id, onFocus = { nav.openFocus(id) }) { sheetTask = null }
    }
}

/** A task's sheet, with the focus spent on it. Starting focus from it closes the sheet. */
@Composable
private fun TaskSheetHost(
    tasks: TasksViewModel,
    focus: FocusViewModel,
    id: String,
    onFocus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focused by remember(id) { focus.focused(id) }.collectAsStateWithLifecycle(TaskFocusTotal.None)
    TaskSheet(
        viewModel = tasks,
        taskId = id,
        focused = focused,
        onFocus = {
            onDismiss()
            onFocus()
        },
        onDismiss = onDismiss,
    )
}

/**
 * The now-focusing bar while a round runs or a break waits, on every screen but the timer's own; null
 * when there is nothing to show.
 */
@Composable
private fun nowFocusing(focus: FocusViewModel, route: String, onOpen: (String) -> Unit): (@Composable () -> Unit)? {
    val timer by focus.timer.collectAsStateWithLifecycle()
    val title by focus.activeTitle.collectAsStateWithLifecycle()
    val taskId = timer.taskId
    if (!timer.isActive || taskId == null || route == Route.focus(taskId)) return null
    return { NowFocusingBar(timer, title.orEmpty(), focus::now) { onOpen(taskId) } }
}

/** Whether [route] has the + button that adds tasks: the dated views and every list. */
private fun showsAdd(route: String): Boolean = route in Route.takesTasks || route.listId() != null

/** Whether [route] keeps room at its bottom for the + button: only while the button shows. */
internal fun clearsAddButton(route: String, addBarOpen: Boolean): Boolean = showsAdd(route) && !addBarOpen

/** The selection bar while [selecting] several tasks, else [route]'s title bar. */
@Composable
private fun RootTopBar(
    route: String,
    selecting: Boolean,
    selection: Selection,
    tasks: TasksViewModel,
    nav: Navigation,
    onReschedule: (Collection<String>) -> Unit,
) {
    if (selecting) {
        SelectionBar(selection, tasks, onReschedule)
        return
    }
    TopBar(route, tasks, nav::back, nav::open)
}

/**
 * The pages of the back stack, each its [topBar] over its screen, moved as one by [transition] while the
 * bars at the bottom stay put. A page with the + button keeps its last row clear of it; while [typing] a
 * tap on the screen only closes the keyboard.
 */
@Composable
private fun Pages(
    transition: Transition<List<String>>,
    screens: Screens,
    topBar: @Composable (String) -> Unit,
    padding: PaddingValues,
    input: PageInput,
) {
    transition.AnimatedContent(transitionSpec = { pageTransform() }, contentKey = { it.last() }) { stack ->
        val shown = stack.last()
        Scaffold(
            modifier = Modifier.tabSwipe(input.tabSeek, shown, input.onTab),
            topBar = { topBar(shown) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
        ) { inner ->
            val page = PaddingValues(top = inner.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            // The bars below already make room for the keyboard while the add bar is open; marking that
            // room as used keeps a page's own keyboard padding (a centred message) from counting it twice.
            val below = PaddingValues(bottom = padding.calculateBottomPadding())
            KeyboardGuardBox(alsoWhen = input.typing, Modifier.consumeWindowInsets(below)) {
                val clear = clearsAddButton(shown, input.addBarOpen)
                RouteContent(shown, screens, if (clear) page.clearOfAddButton() else page)
            }
        }
    }
}

/**
 * What the pages need from the bars around them: whether the add bar is [typing] or [addBarOpen], and
 * the tab swipe ([onTab] null while it is off).
 */
private class PageInput(
    val typing: Boolean,
    val addBarOpen: Boolean,
    val tabSeek: TabSeek,
    val onTab: ((String) -> Unit)?,
)

/** Acts on what a notification opened: a task's sheet, or the focus timer. */
@Composable
private fun OpenedEffect(
    opened: MainActivity.Opened?,
    onTask: (String) -> Unit,
    onFocus: (String) -> Unit,
    onHandled: () -> Unit,
) {
    LaunchedEffect(opened) {
        when (opened) {
            is MainActivity.Opened.Task -> onTask(opened.id)
            is MainActivity.Opened.Focus -> onFocus(opened.taskId)
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

/** Room left under a screen's content so its last rows can scroll clear of the round add button. */
internal val AddButtonClearance = 80.dp

/** What the screens need from the shell. */
private class Screens(
    val tasks: TasksViewModel,
    val settings: SettingsViewModel,
    val focus: FocusViewModel,
    val ctx: RowContext,
    val upcoming: UpcomingState,
    val calendar: CalendarState,
    val nav: Navigation,
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
    val focusTask = route.focusTaskId()
    when {
        list != null -> ListContent(s.tasks, list, s.ctx, padding)

        focusTask != null -> FocusScreen(
            s.focus,
            focusTask,
            padding,
            onComplete = {
                s.tasks.setDone(listOf(focusTask), done = true)
                s.nav.back()
            },
            onGone = s.nav::back,
        )

        route == Route.TODAY -> TodayContent(s.tasks, s.ctx, padding)

        route == Route.UPCOMING -> UpcomingContent(s.tasks, s.ctx, s.upcoming, padding)

        route == Route.CALENDAR -> CalendarContent(s.tasks, s.ctx, s.calendar, padding)

        route == Route.LISTS -> ListsContent(s.tasks, padding) { s.nav.open(Route.list(it)) }

        route == Route.SEARCH -> SearchContent(s.tasks, s.ctx, padding)

        route == Route.SETTINGS -> SettingsScreen(
            s.settings,
            padding,
            onHealth = { s.nav.open(Route.HEALTH) },
            onFocusStats = { s.nav.open(Route.FOCUS_STATS) },
        )

        route == Route.HEALTH -> HealthScreen(s.settings, padding)

        route == Route.FOCUS_STATS -> FocusStatsScreen(s.focus, padding)
    }
}

/**
 * The add bar or the now-focusing bar above the tabs. While typing the tabs step aside, so the add bar
 * sits on the keyboard. While [picking] (a picker hides the keyboard) the bar keeps the keyboard's room,
 * so it stays still behind the picker and drops only if the keyboard doesn't come back.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomBars(
    route: String,
    showAdd: Boolean,
    picking: Boolean,
    onTab: (String) -> Unit,
    nowFocusing: (@Composable () -> Unit)?,
    addBar: @Composable () -> Unit,
) {
    val typing = WindowInsets.isImeVisible
    val focus = LocalFocusManager.current
    // Closing the keyboard ends the edit, so no cursor is left blinking in the bar.
    LaunchedEffect(typing) { if (!typing) focus.clearFocus() }
    val hold = rememberKeyboardHold(picking)
    val held = showAdd && hold.holding
    val room = when {
        held -> WindowInsets.ime.union(WindowInsets(bottom = hold.height))
        typing -> WindowInsets.ime
        else -> null
    }
    Column(room?.let { Modifier.windowInsetsPadding(it) } ?: Modifier) {
        val raised = typing || held
        val focusing = nowFocusing.takeIf { !showAdd && !raised }
        if (showAdd) addBar()
        focusing?.invoke()
        if (route in Route.top && !raised) {
            TabBar(route, onTab)
        } else if (showAdd || focusing != null) {
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
    val open = lists.firstOrNull { it.id == listId }
    val placeholder = open?.let {
        stringResource(
            R.string.add_to_list,
            it.name,
        )
    } ?: stringResource(R.string.add_placeholder)
    val target = parsed.listName ?: open?.name ?: lists.firstOrNull { it.isDefault }?.name.orEmpty()
    QuickAddBar(
        state = state,
        parsed = parsed,
        due = due(parsed, all, state.excluded.keys, preview, defaultOff),
        today = now.toLocalDate(),
        placeholder = placeholder,
        options = AddOptions(lists.map { it.name }, target),
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
    val top = route in Route.top
    // A list is a place of its own, so it gets the same large title as the tabs.
    val large = top || list != null
    val headroom = if (large) titleHeadroom(windowHeight()) else 0.dp
    // Opaque up to the screen's top edge, so rows scrolled up pass under it instead of showing through.
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(
            start = if (top) 16.dp else 4.dp,
            end = 4.dp,
            top = 8.dp + headroom,
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
            large = large,
            modifier = Modifier.weight(1f),
        )
        if (list != null) ListMenu(list, onRename = { renaming = true }) { deleting = true }
        if (top) TopActions(onOpen)
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

/** The icons at the right of a top-level title: search and settings. */
@Composable
private fun TopActions(onOpen: (String) -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = { onOpen(Route.SEARCH) }) {
        Icon(AppIcons.Search, stringResource(R.string.search_title), tint = tint)
    }
    IconButton(onClick = { onOpen(Route.SETTINGS) }) {
        Icon(AppIcons.Settings, stringResource(R.string.settings_title), tint = tint)
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun windowHeight(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

/**
 * Space above a large title, so on a tall phone the first rows sit lower, nearer the thumb. Nothing on a
 * short screen; it grows with the height and stops at [MAX_HEADROOM].
 */
internal fun titleHeadroom(windowHeight: Dp): Dp =
    ((windowHeight - SHORT_SCREEN) * HEADROOM_SHARE).coerceIn(0.dp, MAX_HEADROOM)

private val SHORT_SCREEN = 640.dp
private const val HEADROOM_SHARE = 0.16f
private val MAX_HEADROOM = 48.dp

private fun titleOf(route: String): Int = when (route) {
    Route.TODAY -> R.string.today_title
    Route.UPCOMING -> R.string.upcoming_title
    Route.CALENDAR -> R.string.calendar_title
    Route.LISTS -> R.string.lists_title
    Route.SEARCH -> R.string.search_title
    Route.SETTINGS -> R.string.settings_title
    Route.HEALTH -> R.string.health_title
    Route.FOCUS_STATS -> R.string.stats_title
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
private fun SelectionBar(selection: Selection, tasks: TasksViewModel, onReschedule: (Collection<String>) -> Unit) {
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
            onReschedule(selection.ids)
            selection.clear()
        }) { Icon(AppIcons.Reschedule, stringResource(R.string.action_reschedule), tint = scheme.onPrimaryContainer) }
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
            Tab(icon, stringResource(label), selected = route == target) { onTab(target) }
        }
    }
}

@Composable
private fun RowScope.Tab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.secondary else scheme.onSurfaceVariant
    val text = if (selected) scheme.secondary else scheme.onSurfaceVariant
    Column(
        Modifier.weight(1f).clickable(role = Role.Tab, onClick = onClick).padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = text,
        )
    }
}

/** A hairline along the top edge. */
private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx() / 2)
}
