package com.aliraza.dailytodos.ui.components

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

    @Test
    fun aQuickFlickActsShortOfTheThresholdButOnlyTheWayTheRowMoved() {
        assertEquals(SwipeAction.FOCUS, releaseAction(40f, width, velocity = 1200f, fling = 800f))
        assertEquals(SwipeAction.DELETE, releaseAction(-40f, width, velocity = -1200f, fling = 800f))
        assertNull(releaseAction(40f, width, velocity = -1200f, fling = 800f))
        assertNull(releaseAction(40f, width, velocity = 300f, fling = 800f))
        assertNull(releaseAction(40f, 0f, velocity = 1200f, fling = 800f))
    }
}
