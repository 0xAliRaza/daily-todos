package com.aliraza.dailytodos.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabSwipeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun leftGoesToTheNextTabAndRightToThePrevious() {
        assertEquals(Route.CALENDAR, swipedTab(Route.UPCOMING, dx = -200f, threshold = 100f))
        assertEquals(Route.TODAY, swipedTab(Route.UPCOMING, dx = 200f, threshold = 100f))
    }

    @Test
    fun shortSwipesEndsAndOtherScreensStay() {
        assertNull(swipedTab(Route.UPCOMING, dx = -50f, threshold = 100f))
        assertNull(swipedTab(Route.TODAY, dx = 200f, threshold = 100f))
        assertNull(swipedTab(Route.LISTS, dx = -200f, threshold = 100f))
        assertNull(swipedTab(Route.SETTINGS, dx = -200f, threshold = 100f))
    }

    @Test
    fun anEmptyAreaSwitchesTabsButASwipeableRowKeepsItsSwipe() {
        val picked = mutableListOf<String>()
        compose.setContent {
            Column(Modifier.size(400.dp).tabSwipe(Route.UPCOMING) { picked += it }) {
                Box(
                    Modifier.fillMaxWidth().height(100.dp).testTag("row")
                        .draggable(rememberDraggableState {}, Orientation.Horizontal),
                )
                Box(Modifier.fillMaxWidth().height(300.dp).testTag("empty"))
            }
        }
        compose.onNodeWithTag("row").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("empty").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("empty").performTouchInput { swipeRight() }
        assertEquals(listOf(Route.CALENDAR, Route.TODAY), picked)
    }
}
