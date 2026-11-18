package com.aliraza.dailytasks.ui

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
    const val FOCUS = "focus"
    const val LIST_PREFIX = "list/"

    val tabs = listOf(TODAY, UPCOMING, LISTS)

    /** Upcoming as a list or as a calendar: one place, two views, under one tab. */
    val upcomingViews = setOf(UPCOMING, CALENDAR)

    /** Where the + button adds tasks (lists too, by their own routes). */
    val takesTasks = setOf(TODAY, UPCOMING, CALENDAR)

    /** Top-level places: the tabs and the calendar view; they get the large title and the tab bar. */
    val top = tabs.toSet() + CALENDAR
}

/**
 * The back stack, and the rules for moving through it. A tab starts a fresh stack; the Upcoming tab
 * reopens whichever of its two views was used last while the app runs, and starts as the list whenever
 * the app opens again (the view isn't saved).
 */
@Stable
internal class Navigation(stack: List<String>, upcomingView: String = Route.UPCOMING) {
    var stack by mutableStateOf(stack)
        private set

    var upcomingView by mutableStateOf(upcomingView)
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
        stack = listOf(if (target == Route.UPCOMING) upcomingView else target)
    }

    /** The timer from a notification: on top, once, wherever the app was. */
    fun openFocus() {
        stack = stack.filter { it != Route.FOCUS } + Route.FOCUS
    }

    /** Flips Upcoming between its list and its calendar, and returns the view now shown. */
    fun switchUpcomingView(): String {
        upcomingView = if (route == Route.CALENDAR) Route.UPCOMING else Route.CALENDAR
        stack = listOf(upcomingView)
        return upcomingView
    }

    companion object {
        /**
         * Keeps the screen across rotation and theme changes, which recreate the activity. A fresh start
         * of the app saves nothing, so Upcoming then opens as its list.
         */
        val Saver = listSaver<Navigation, String>(
            save = { listOf(it.upcomingView) + it.stack },
            restore = { Navigation(it.drop(1), it.first()) },
        )
    }
}

@Composable
internal fun rememberNavigation(): Navigation =
    rememberSaveable(saver = Navigation.Saver) { Navigation(listOf(Route.TODAY)) }
