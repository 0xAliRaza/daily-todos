package com.aliraza.dailytasks.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NavigationTest {

    private fun fresh() = Navigation(listOf(Route.TODAY))

    @Test
    fun theUpcomingTabStartsAsTheListThenReopensTheViewUsedLast() {
        val nav = fresh()
        nav.tab(Route.UPCOMING)
        assertEquals(Route.UPCOMING, nav.route)
        assertEquals(Route.CALENDAR, nav.switchUpcomingView())
        nav.tab(Route.LISTS)
        nav.tab(Route.UPCOMING)
        assertEquals(Route.CALENDAR, nav.route)
        assertEquals(Route.UPCOMING, nav.switchUpcomingView())
        nav.tab(Route.TODAY)
        nav.tab(Route.UPCOMING)
        assertEquals(Route.UPCOMING, nav.route)
    }

    @Test
    fun switchingViewsReplacesRatherThanStacks() {
        val nav = fresh()
        nav.tab(Route.UPCOMING)
        nav.switchUpcomingView()
        assertFalse(nav.canGoBack)
    }

    @Test
    fun theTimerFromANotificationSitsOnTopOnce() {
        val nav = fresh()
        nav.open(Route.FOCUS)
        nav.open(Route.SETTINGS)
        nav.openFocus()
        assertEquals(listOf(Route.TODAY, Route.SETTINGS, Route.FOCUS), nav.stack)
        nav.back()
        nav.back()
        nav.back()
        assertEquals(listOf(Route.TODAY), nav.stack)
    }

    @Test
    fun rotatingKeepsTheCalendarAndTheViewUsedLast() {
        val nav = fresh()
        nav.tab(Route.UPCOMING)
        nav.switchUpcomingView()
        val saved = with(Navigation.Saver) { SaverScope { true }.save(nav) }
        val restored = Navigation.Saver.restore(saved!!)!!
        assertEquals(Route.CALENDAR, restored.route)
        restored.tab(Route.LISTS)
        restored.tab(Route.UPCOMING)
        assertEquals(Route.CALENDAR, restored.route)
    }
}
