package com.aliraza.dailytodos.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationTest {

    private fun fresh() = Navigation(listOf(Route.TODAY))

    @Test
    fun fourTabsWithUpcomingAndCalendarApart() {
        assertEquals(listOf(Route.TODAY, Route.UPCOMING, Route.CALENDAR, Route.LISTS), Route.tabs)
    }

    @Test
    fun aTabStartsAFreshStack() {
        val nav = fresh()
        nav.open(Route.SETTINGS)
        nav.tab(Route.CALENDAR)
        assertEquals(listOf(Route.CALENDAR), nav.stack)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun aTaskTimerOpensOnTopAndOnlyOnce() {
        val nav = fresh()
        nav.openFocus("t1")
        nav.open(Route.SETTINGS)
        nav.openFocus("t1")
        assertEquals(listOf(Route.TODAY, Route.SETTINGS, Route.focus("t1")), nav.stack)
        nav.back()
        assertEquals(Route.SETTINGS, nav.route)
    }

    @Test
    fun routesNameTheirTaskOrList() {
        assertEquals("t1", Route.focus("t1").focusTaskId())
        assertEquals("l1", Route.list("l1").listId())
        assertNull(Route.TODAY.focusTaskId())
        assertNull(Route.focus("t1").listId())
    }

    @Test
    fun rotatingKeepsTheStack() {
        val nav = fresh()
        nav.tab(Route.UPCOMING)
        nav.openFocus("t1")
        val saved = with(Navigation.Saver) { SaverScope { true }.save(nav) }
        val restored = Navigation.Saver.restore(saved!!)!!
        assertEquals(nav.stack, restored.stack)
    }
}
