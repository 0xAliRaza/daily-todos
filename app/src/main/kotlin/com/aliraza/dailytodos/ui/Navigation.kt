package com.aliraza.dailytodos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Where the app is. A plain back stack of these; [tabs] are the bottom tabs. */
internal object Route {
    const val TODAY = "today"
    const val UPCOMING = "upcoming"
    const val CALENDAR = "calendar"
    const val LISTS = "lists"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val HEALTH = "health"
    const val FOCUS_STATS = "focus-stats"
    const val LIST_PREFIX = "list/"

    /** A task's focus timer. */
    const val FOCUS_PREFIX = "focus/"

    val tabs = listOf(TODAY, UPCOMING, CALENDAR, LISTS)

    /** Where the + button adds tasks (lists too, by their own routes). */
    val takesTasks = setOf(TODAY, UPCOMING, CALENDAR)

    /** Top-level places: the tabs; they get the large title and the tab bar. */
    val top = tabs.toSet()

    fun list(id: String) = LIST_PREFIX + id

    fun focus(taskId: String) = FOCUS_PREFIX + taskId
}

internal fun String.listId(): String? = removePrefix(Route.LIST_PREFIX).takeIf { startsWith(Route.LIST_PREFIX) }

internal fun String.focusTaskId(): String? = removePrefix(Route.FOCUS_PREFIX).takeIf { startsWith(Route.FOCUS_PREFIX) }

/** The back stack, and the rules for moving through it. A tab starts a fresh stack. */
@Stable
internal class Navigation(stack: List<String>) {
    var stack by mutableStateOf(stack)
        private set

    val route: String get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun open(next: String) {
        stack = stack + next
    }

    fun back() {
        if (canGoBack) stack = stack.dropLast(1)
    }

    fun tab(target: String) {
        stack = listOf(target)
    }

    /**
     * Opens [taskId]'s timer on top of where the app is. Opened again (from its notification, say) it moves
     * to the top rather than stacking a second copy, so back always leaves the timer in one step.
     */
    fun openFocus(taskId: String) {
        val timer = Route.focus(taskId)
        stack = stack.filter { it != timer } + timer
    }

    companion object {
        /** Keeps the screen across rotation and theme changes, which recreate the activity. */
        val Saver = listSaver<Navigation, String>(save = { it.stack }, restore = { Navigation(it) })
    }
}

@Composable
internal fun rememberNavigation(): Navigation =
    rememberSaveable(saver = Navigation.Saver) { Navigation(listOf(Route.TODAY)) }
