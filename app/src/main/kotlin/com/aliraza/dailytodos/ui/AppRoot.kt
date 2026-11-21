package com.aliraza.dailytodos.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.aliraza.dailytodos.ui.components.HeaderCollapse
import com.aliraza.dailytodos.ui.components.KeyboardGuardBox
import com.aliraza.dailytodos.ui.components.LargeHeader
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.components.Notice
import com.aliraza.dailytodos.ui.components.NoticeCard
import com.aliraza.dailytodos.ui.components.QuickAddBar
import com.aliraza.dailytodos.ui.components.QuickAddState
import com.aliraza.dailytodos.ui.components.due
import com.aliraza.dailytodos.ui.components.longDateLabel
import com.aliraza.dailytodos.ui.components.rememberHeaderCollapse
import com.aliraza.dailytodos.ui.components.startWords
import com.aliraza.dailytodos.ui.components.switchedOffWhen
import com.aliraza.dailytodos.ui.focus.FocusScreen
import com.aliraza.dailytodos.ui.focus.FocusViewModel
import com.aliraza.dailytodos.ui.focus.NowFocusingBar
import com.aliraza.dailytodos.ui.focus.barActions
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
import android.graphics.Color as AndroidColor

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
    var moving by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val route = nav.route
    val selection = remember { Selection() }
    // Read through derivedStateOf so the root recomposes when the date turns, not every minute.
    val clock = tasks.now.collectAsStateWithLifecycle()
    val today by remember { derivedStateOf { clock.value.toLocalDate() } }
    val weekStart = LocalWeekStart.current
    val upcoming = remember(today, weekStart) { UpcomingState(today, weekStart) }
    val calendar = remember { CalendarState(tasks.now.value.toLocalDate()) }
    LaunchedEffect(route) { selection.clear() }
    SystemBarsFollowTheme()
    OpenedEffect(
        opened,
        onTask = { sheetTask = it },
        onFocus = nav::openFocus,
        onHandled = onOpenedHandled,
    )
    // Clearing a selection wins over going back; a back swipe otherwise lets the page below peek in.
    BackHandler(enabled = selection.active) { selection.clear() }
    val tabSeek = rememberTabSeek()
    val pages = rememberPageTransition(nav, backEnabled = !selection.active, tabSeek)

    val snackbar = remember { SnackbarHostState() }
    UndoSnackbars(tasks, snackbar)
    FocusNotices(focus, snackbar)
    val ctx = rememberRowContext(
        tasks,
        selection,
        onReschedule = { if (it.isNotEmpty()) rescheduling = ArrayList(it) },
        onFocus = nav::openFocus,
    ) { sheetTask = it }
    val quickAdd = remember { QuickAddState() }
    val canAdd = showsAdd(route) && !selection.active
    val addContext = contextDate(route, upcoming, calendar)
    val addStart = rememberStartWords(tasks, route, addContext)
    val topBar: @Composable (String, HeaderCollapse) -> Unit = { shown, collapse ->
        // A page sliding away keeps its title; only the page in front shows the selection, fading in.
        AnimatedContent(
            selection.active && shown == route,
            transitionSpec = { fadeIn(tween(QUICK_MS)) togetherWith fadeOut(tween(QUICK_MS)) },
            label = "topBar",
        ) { selecting ->
            if (selecting) {
                SelectionHeader(selection, rememberVisibleIds(shown, tasks, calendar))
            } else {
                TopBar(shown, collapse, tasks, nav)
            }
        }
    }
    val selectionActions = selectionActions(selection, tasks, onMove = { moving = it }) {
        if (it.isNotEmpty()) rescheduling = ArrayList(it)
    }

    val addLift = remember { mutableStateOf(0.dp) }
    val lifted by animateDpAsState(addLift.value, label = "addLift")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data -> NoticeCard(data) }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = canAdd && !quickAdd.open,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(tween(QUICK_MS)) + fadeOut(tween(QUICK_MS)),
            ) { AddButton(Modifier.padding(bottom = lifted)) { quickAdd.show(addStart) } }
        },
        bottomBar = {
            BottomBars(
                route,
                showAdd = canAdd && quickAdd.open,
                picking = quickAdd.picking,
                onTab = nav::tab,
                nowFocusing = nowFocusing(focus, route, nav::openFocus),
                selecting = selectionActions.takeIf { selection.active },
            ) {
                AddBar(tasks, quickAdd, route, addContext)
            }
        },
    ) { padding ->
        val screens = Screens(tasks, settings, focus, ctx, upcoming, calendar, nav)
        val onTab = nav::tab.takeIf { !selection.active && !quickAdd.open }
        CompositionLocalProvider(LocalAddButtonLift provides addLift) {
            Pages(pages, screens, topBar, padding, PageInput(route, quickAdd.focused, quickAdd.open, tabSeek, onTab))
        }
    }
    rescheduling?.let { ids -> RescheduleHost(tasks, ids) { rescheduling = null } }
    moving?.let { ids ->
        MoveSheet(
            tasks,
            onPick = { list ->
                tasks.moveToList(ids, list)
                selection.clear()
            },
        ) { moving = null }
    }
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
    return { NowFocusingBar(timer, title.orEmpty(), focus::now, focus.barActions()) { onOpen(taskId) } }
}

/** Whether [route] has the + button that adds tasks: the dated views and every list. */
private fun showsAdd(route: String): Boolean = route in Route.takesTasks || route.listId() != null

/** Whether [route] keeps room at its bottom for the + button: only while the button shows. */
internal fun clearsAddButton(route: String, addBarOpen: Boolean): Boolean = showsAdd(route) && !addBarOpen

/** The bottom bar's actions on the picked tasks; Move keeps the pick until a list is chosen. */
private fun selectionActions(
    selection: Selection,
    tasks: TasksViewModel,
    onMove: (List<String>) -> Unit,
    onReschedule: (Collection<String>) -> Unit,
): @Composable () -> Unit = {
    SelectionActions(
        onComplete = {
            tasks.setDone(selection.ids, done = true)
            selection.clear()
        },
        onReschedule = {
            onReschedule(selection.ids)
            selection.clear()
        },
        onMove = { onMove(selection.ids.toList()) },
        onDelete = {
            tasks.delete(selection.ids)
            selection.clear()
        },
    )
}

/**
 * Status and navigation bar icons dark on a light theme and light on a dark one, following the app's
 * own theme rather than the phone's, which differ when a theme is picked in settings.
 */
@Composable
private fun SystemBarsFollowTheme() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val dark = MaterialTheme.tokens.background.luminance() < HALF_LUMINANCE
    LaunchedEffect(activity, dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
    }
}

/** The scrims Android's own edge-to-edge default puts behind three-button navigation. */
private const val LIGHT_SCRIM = 0xE6FFFFFF.toInt()
private const val DARK_SCRIM = 0x801B1B1B.toInt()
private const val HALF_LUMINANCE = 0.5f

/**
 * The pages of the back stack, each its [topBar] over its screen, moved as one by [transition] while the
 * bars at the bottom stay put. A page with the + button keeps its last row clear of it; while [typing] a
 * tap on the screen only closes the keyboard.
 */
@Composable
private fun Pages(
    transition: Transition<List<String>>,
    screens: Screens,
    topBar: @Composable (String, HeaderCollapse) -> Unit,
    padding: PaddingValues,
    input: PageInput,
) {
    // The swipe is read here, outside the pages: the pages move with the finger, so a reading taken on
    // them would see only part of each move and the page would trail behind it.
    transition.AnimatedContent(
        transitionSpec = { pageTransform() },
        contentKey = { it.last() },
        modifier = Modifier.tabSwipe(input.tabSeek, input.route, input.onTab),
    ) { stack ->
        val shown = stack.last()
        // Each page folds its own header as its content scrolls.
        val collapse = rememberHeaderCollapse()
        Scaffold(
            modifier = Modifier
                .graphicsLayer { translationX = input.tabSeek.stretch }
                .nestedScroll(collapse.connection),
            topBar = { topBar(shown, collapse) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
        ) { inner ->
            val clear = clearsAddButton(shown, input.addBarOpen)
            val page = PaddingValues(
                top = inner.calculateTopPadding(),
                bottom = pageBottom(padding.calculateBottomPadding(), clear),
            )
            KeyboardGuardBox(alsoWhen = input.typing) { RouteContent(shown, screens, page) }
        }
    }
}

/**
 * What the pages need from around them: the [route] in front, whether the add bar is [typing] or
 * [addBarOpen], and the tab swipe ([onTab] null while it is off).
 */
private class PageInput(
    val route: String,
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
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.tokens.accent,
        contentColor = MaterialTheme.tokens.onAccent,
    ) {
        Icon(AppIcons.Add, stringResource(R.string.add_placeholder), modifier = Modifier.size(24.dp))
    }
}

/**
 * Room under a page's last row: the [bars] below it (tabs, the now-focusing bar, the add bar), and room
 * for the + button over them while it [clearsButton], so the button never covers the last row.
 */
internal fun pageBottom(bars: Dp, clearsButton: Boolean): Dp = if (clearsButton) bars + AddButtonClearance else bars

/** How far the page in front lifts the + button; see [LiftAddButton]. */
internal val LocalAddButtonLift = staticCompositionLocalOf { mutableStateOf(0.dp) }

/**
 * Lifts the + button by [by] while the calling page shows, so it clears a bar pinned at the page's
 * bottom (the Completed bar) instead of covering it. The snackbar rises with it.
 */
@Composable
internal fun LiftAddButton(by: Dp) {
    val lift = LocalAddButtonLift.current
    DisposableEffect(lift, by) {
        lift.value = by
        onDispose { lift.value = 0.dp }
    }
}

/** Quick exits and fades: the + button, the add bar leaving, the selection bar. */
private const val QUICK_MS = 150

/** The 56 dp + button with 16 dp over and under it: what a page's last row scrolls clear of. */
internal val AddButtonClearance = 88.dp

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

        route == Route.TODAY -> TodayContent(s.tasks, s.ctx, padding) {
            s.upcoming.home()
            s.nav.tab(Route.UPCOMING)
        }

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
    selecting: (@Composable () -> Unit)?,
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
        // While picking tasks their actions take the tabs' place, on every screen.
        val actions = selecting.takeIf { !raised }
        val focusing = nowFocusing.takeIf { !showAdd && !raised && actions == null }
        AnimatedVisibility(
            visible = showAdd,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(tween(QUICK_MS), shrinkTowards = Alignment.Bottom) + fadeOut(tween(QUICK_MS)),
        ) { addBar() }
        focusing?.invoke()
        if (actions != null) {
            actions()
        } else if (route in Route.top && !raised) {
            TabBar(route, onTab)
        } else if (showAdd || focusing != null) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** The timer's short words on what a tap did, in the same snackbar as undo. */
@Composable
private fun FocusNotices(focus: FocusViewModel, host: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(focus) {
        focus.notices.collectLatest { notice ->
            host.showSnackbar(
                Notice(resources.getString(notice.title), resources.getString(notice.detail), notice.icon),
            )
        }
    }
}

@Composable
private fun UndoSnackbars(tasks: TasksViewModel, host: SnackbarHostState) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(tasks) {
        tasks.undo.collectLatest { event ->
            val result = host.showSnackbar(Notice(event.text(resources), actionLabel = undoLabel))
            if (result == SnackbarResult.ActionPerformed) event.undo()
        }
    }
}

/**
 * The words the add bar's line starts with on [route]: the open list's tag and the picked [contextDate].
 * The default list gets no tag, and tomorrow is left to the default-date pill where it would show anyway.
 */
@Composable
private fun rememberStartWords(tasks: TasksViewModel, route: String, contextDate: LocalDate?): String {
    val now by tasks.now.collectAsStateWithLifecycle()
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val settings by tasks.settings.collectAsStateWithLifecycle()
    val open = lists.firstOrNull { it.id == route.listId() }?.takeIf { !it.isDefault }
    val today = now.toLocalDate()
    val defaultDay = today.plusDays(1).takeIf { settings.undatedGetsTomorrow && open == null }
    return startWords(open?.name, contextDate, today, defaultDay)
}

@Composable
private fun AddBar(tasks: TasksViewModel, state: QuickAddState, route: String, contextDate: LocalDate?) {
    val now by tasks.now.collectAsStateWithLifecycle()
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val settings by tasks.settings.collectAsStateWithLifecycle()
    val listId = route.listId()
    val parsed = remember(state.text, state.excluded, lists, now) { state.read(state.excluded.keys, tasks::parse) }
    val all = remember(state.text, lists, now) { state.read(emptySet(), tasks::parse) }
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
            val line = state.read(state.excluded.keys, tasks::parse)
            if (line.title.isNotBlank()) {
                val off = state.defaultDateOff ||
                    switchedOffWhen(state.read(emptySet(), tasks::parse), state.excluded.keys).isNotEmpty()
                tasks.add(line, listId, tasks.defaultDate(line, settings, listId, contextDate, off))
                state.clear()
            }
        },
    )
}

/**
 * [route]'s header: the large title over its subtitle (the date on Today, the open count in a list),
 * folding with [collapse]. Tabs carry search and settings; pushed pages a back arrow, a list its menu.
 * The timer has only the back arrow: the task's own title heads its page.
 */
@Composable
private fun TopBar(route: String, collapse: HeaderCollapse, tasks: TasksViewModel, nav: Navigation) {
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val list = route.listId()?.let { id -> lists.firstOrNull { it.id == id } }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val top = route in Route.top
    val timer = route.focusTaskId() != null
    LargeHeader(
        title = if (timer) "" else list?.name ?: stringResource(titleOf(route)),
        subtitle = subtitleOf(route, tasks),
        collapse = collapse,
        large = !timer,
        navigation = if (top) null else ({ BackButton(nav::back) }),
    ) {
        if (list != null) ListMenu(list, onRename = { renaming = true }) { deleting = true }
        if (top) TopActions(nav::open)
    }
    if (renaming && list != null) {
        RenameDialog(list.name, onDismiss = { renaming = false }) { tasks.renameList(list.id, it) }
    }
    if (deleting && list != null) {
        DeleteListDialog(list.name, onDismiss = { deleting = false }) {
            tasks.deleteList(list.id)
            nav.back()
        }
    }
}

/** The line under a large title: the long date on Today, "5 open" in a list, nothing elsewhere. */
@Composable
private fun subtitleOf(route: String, tasks: TasksViewModel): String? {
    val listId = route.listId()
    return when {
        route == Route.TODAY -> {
            val now by tasks.now.collectAsStateWithLifecycle()
            longDateLabel(now.toLocalDate())
        }

        listId != null -> {
            val counts by tasks.listCounts.collectAsStateWithLifecycle()
            val open = counts[listId] ?: 0
            pluralStringResource(R.plurals.list_open_count, open, open)
        }

        else -> null
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(AppIcons.Back, stringResource(R.string.action_back), tint = MaterialTheme.tokens.ink)
    }
}

/** The icons at the right of a tab's header: search and settings. */
@Composable
private fun TopActions(onOpen: (String) -> Unit) {
    val tint = MaterialTheme.tokens.ink
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
            Icon(AppIcons.More, stringResource(R.string.action_more), tint = MaterialTheme.tokens.ink)
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
