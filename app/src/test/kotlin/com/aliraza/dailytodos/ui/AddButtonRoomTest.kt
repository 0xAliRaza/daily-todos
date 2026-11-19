package com.aliraza.dailytodos.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pages keep room for the round + button only while it shows, so an open add bar leaves no gap. */
class AddButtonRoomTest {
    @Test
    fun roomForTheButtonOnPagesThatShowIt() {
        assertTrue(clearsAddButton(Route.TODAY, addBarOpen = false))
        assertFalse(clearsAddButton(Route.SETTINGS, addBarOpen = false))
    }

    @Test
    fun noRoomWhileTheAddBarIsOpen() {
        assertFalse(clearsAddButton(Route.TODAY, addBarOpen = true))
    }
}
