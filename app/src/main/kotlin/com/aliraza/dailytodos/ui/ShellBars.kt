package com.aliraza.dailytodos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.calendar.CalendarMode
import com.aliraza.dailytodos.ui.calendar.CalendarState
import com.aliraza.dailytodos.ui.calendar.visibleDays
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.AppSheet
import com.aliraza.dailytodos.ui.components.HeaderBarHeight
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.tasks.Selection
import com.aliraza.dailytodos.ui.tasks.TasksViewModel
import com.aliraza.dailytodos.ui.theme.barTitle
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.tab
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.time.LocalDate

/** The four tabs, in order, with their icons and names. */
private val Tabs = listOf(
    Triple(Route.TODAY, AppIcons.Tab.TODAY, R.string.today_title),
    Triple(Route.UPCOMING, AppIcons.Tab.UPCOMING, R.string.upcoming_title),
    Triple(Route.CALENDAR, AppIcons.Tab.CALENDAR, R.string.calendar_title),
    Triple(Route.LISTS, AppIcons.Tab.LISTS, R.string.lists_title),
)

/** The tab bar: the picked tab's icon filled on an accent-container pill, its name bold in the accent. */
@Composable
internal fun TabBar(route: String, onTab: (String) -> Unit) {
    BarRow {
        Tabs.forEach { (target, tab, label) ->
            val selected = route == target
            val tokens = MaterialTheme.tokens
            val icon = if (selected) {
                AppIcons.selectedTab(tab, tokens.accent, tokens.accentContainer)
            } else {
                AppIcons.tab(tab)
            }
            BarItem(
                icon = icon,
                label = stringResource(label),
                color = if (selected) tokens.accent else tokens.ink2,
                selected = selected,
                role = Role.Tab,
            ) { onTab(target) }
        }
    }
}

/** What the tab bar turns into while several tasks are picked: Complete, Reschedule, Move and Delete. */
@Composable
internal fun SelectionActions(
    onComplete: () -> Unit,
    onReschedule: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    BarRow {
        BarItem(AppIcons.Check, stringResource(R.string.action_complete), tokens.ink, onClick = onComplete)
        BarItem(AppIcons.Reschedule, stringResource(R.string.action_reschedule), tokens.ink, onClick = onReschedule)
        BarItem(AppIcons.Move, stringResource(R.string.action_move), tokens.ink, onClick = onMove)
        BarItem(AppIcons.Delete, stringResource(R.string.action_delete), tokens.danger, onClick = onDelete)
    }
}

/**
 * The 80 dp bar along the bottom: a top hairline, 8 dp over the items and 16 dp under them, or the
 * navigation bar's own height where that is more.
 */
@Composable
private fun BarRow(content: @Composable RowScope.() -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().background(tokens.background).topHairline(tokens.hairline)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets(bottom = BarBottom)))
            .padding(top = BarTop).height(BarItemsHeight),
        content = content,
    )
}

/** One item of a bottom bar: a 24 dp icon on a 56 × 32 pill, its label under it. */
@Composable
private fun RowScope.BarItem(
    icon: ImageVector,
    label: String,
    color: Color,
    selected: Boolean = false,
    role: Role = Role.Button,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    val tint by animateColorAsState(color, label = "barItem")
    val pill by animateColorAsState(if (selected) tokens.accentContainer else Color.Transparent, label = "barPill")
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(role = role, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(width = 56.dp, height = 32.dp).background(pill, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // The picked tab's icon carries its own two colours; the others take the tint.
            Icon(icon, null, tint = if (selected) Color.Unspecified else tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.tab,
            fontWeight = if (selected) FontWeight.Bold else null,
            color = tint,
            maxLines = 1,
        )
    }
}

/** The header while picking: ×, "2 selected", and Select all for everything the screen shows. */
@Composable
internal fun SelectionHeader(selection: Selection, visible: List<String>) {
    val tokens = MaterialTheme.tokens
    val count = selection.ids.size
    val missing = visible.filterNot { it in selection.ids }
    Row(
        Modifier.fillMaxWidth().background(tokens.background).statusBarsPadding()
            .bottomHairline(tokens.hairline).height(HeaderBarHeight).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = selection::clear) {
            Icon(AppIcons.Close, stringResource(R.string.action_cancel), tint = tokens.ink)
        }
        Text(
            pluralStringResource(R.plurals.selected_count, count, count),
            style = MaterialTheme.typography.barTitle,
            color = tokens.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val canAdd = missing.isNotEmpty()
        Box(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
                .clickable(enabled = canAdd, role = Role.Button) { missing.forEach(selection::toggle) }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.action_select_all),
                style = MaterialTheme.typography.button,
                color = if (canAdd) tokens.accent else tokens.ink3,
            )
        }
    }
}

/**
 * The open tasks [route] shows, in its order: what Select all picks. The calendar counts the days in its
 * view (the picked day in Month, where only that day's tasks are listed).
 */
@Composable
internal fun rememberVisibleIds(route: String, tasks: TasksViewModel, calendar: CalendarState): List<String> {
    val listId = route.listId()
    val listFlow = remember(listId) { listId?.let(tasks::listTasks) ?: flowOf(emptyList()) }
    val inList by listFlow.collectAsStateWithLifecycle(emptyList())
    val today by tasks.todaySections.collectAsStateWithLifecycle()
    val upcoming by tasks.upcoming.collectAsStateWithLifecycle()
    val byDay by tasks.byDay.collectAsStateWithLifecycle()
    val results by tasks.results.collectAsStateWithLifecycle()
    val weekStart = LocalWeekStart.current
    val visible = when {
        listId != null -> inList
        route == Route.TODAY -> today?.all.orEmpty()
        route == Route.UPCOMING -> upcoming.orEmpty().values.flatten()
        route == Route.SEARCH -> results
        route == Route.CALENDAR -> calendarDays(calendar, weekStart).flatMap { byDay.orEmpty()[it].orEmpty() }
        else -> emptyList()
    }
    return visible.filter { it.completedAt == null }.map { it.id }
}

/** The days whose tasks the calendar lists: the picked day in Month, else every day in view. */
private fun calendarDays(calendar: CalendarState, weekStart: DayOfWeek): List<LocalDate> =
    if (calendar.mode == CalendarMode.MONTH) {
        listOf(calendar.anchor)
    } else {
        visibleDays(calendar.mode, calendar.anchor, weekStart)
    }

/** Picks the list the selected tasks move to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoveSheet(tasks: TasksViewModel, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val lists by tasks.lists.collectAsStateWithLifecycle()
    val tokens = MaterialTheme.tokens
    AppSheet(onDismiss) {
        Text(
            stringResource(R.string.move_title),
            style = MaterialTheme.typography.barTitle,
            color = tokens.ink,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        )
        lists.forEach { list ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) {
                    onPick(list.id)
                    onDismiss()
                }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(AppIcons.Lists, null, tint = tokens.ink2, modifier = Modifier.size(24.dp))
                Text(list.name, style = MaterialTheme.typography.taskTitle, color = tokens.ink)
            }
        }
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(16.dp))
    }
}

/** A hairline along the top edge. */
internal fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}

/** A hairline along the bottom edge. */
private fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val y = size.height - 1.dp.toPx() / 2
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

private val BarTop = 8.dp
private val BarBottom = 16.dp

/** The 80 dp bar less its 8 dp top and 16 dp bottom. */
private val BarItemsHeight = 56.dp
