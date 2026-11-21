package com.aliraza.dailytodos.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pages keep room for the round + button only while it shows, so an open add bar leaves no gap. */
class AddButtonRoomTest {
    @Test
    fun roomForTheButtonOnPagesThatShowIt() {
        assertTrue(clearsAddButton(Route.TODAY, addBarOpen = false))
        assertTrue(clearsAddButton(Route.list("work"), addBarOpen = false))
        assertFalse(clearsAddButton(Route.SETTINGS, addBarOpen = false))
    }

    @Test
    fun noRoomWhileTheAddBarIsOpen() {
        assertFalse(clearsAddButton(Route.TODAY, addBarOpen = true))
    }

    /** 88 dp over the tab bar, and 68 dp more while the now-focusing bar sits between them. */
    @Test
    fun theLastRowClearsTheButtonAndTheBarsUnderIt() {
        val tabs = 80.dp
        val focusBar = 68.dp
        assertEquals(168.dp, pageBottom(tabs, clearsButton = true))
        assertEquals(236.dp, pageBottom(tabs + focusBar, clearsButton = true))
        assertEquals(tabs, pageBottom(tabs, clearsButton = false))
    }
}
