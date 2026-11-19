package com.aliraza.dailytasks.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeActionTest {

    private val width = 1000f

    @Test
    fun aShortSwipeDoesNothing() {
        assertNull(swipeAction(299f, width))
        assertNull(swipeAction(-299f, width))
    }

    @Test
    fun rightPastTheThresholdOpensFocusAndLeftDeletes() {
        assertEquals(SwipeAction.FOCUS, swipeAction(300f, width))
        assertEquals(SwipeAction.DELETE, swipeAction(-300f, width))
    }

    @Test
    fun aRowNotLaidOutYetNeverActs() {
        assertNull(swipeAction(-50f, 0f))
    }
}
