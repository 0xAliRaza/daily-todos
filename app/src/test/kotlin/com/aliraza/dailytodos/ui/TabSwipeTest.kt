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
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun aSwipeTowardsNoTabGivesALittleAndStiffens() {
        assertEquals(0f, rubberBand(0f, limit = 100f), 0.001f)
        val small = rubberBand(20f, limit = 100f)
        val large = rubberBand(400f, limit = 100f)
        assertTrue(small > 0f && small < 20f)
        assertTrue(large > small && large < 100f)
        assertEquals(-small, rubberBand(-20f, limit = 100f), 0.001f)
    }

    @Test
    fun seekingFollowsTheFingerThroughTheEasing() {
        assertEquals(0f, seekFraction(0f), 0.01f)
        assertEquals(1f, seekFraction(1f), 0.01f)
        val half = seekFraction(0.5f)
        assertEquals(0.5f, androidx.compose.animation.core.FastOutSlowInEasing.transform(half), 0.01f)
    }

    @Test
    fun tabsSlideTowardsTheirPlaceInTheBar() {
        assertEquals(1, tabSide(Route.TODAY, Route.CALENDAR))
        assertEquals(-1, tabSide(Route.LISTS, Route.UPCOMING))
        assertEquals(0, tabSide(Route.TODAY, Route.SETTINGS))
    }

    @Test
    fun aShortQuickFlickSwitchesTabsAndAShortSlowDragDoesNot() {
        val picked = mutableListOf<String>()
        compose.setContent {
            Box(Modifier.size(400.dp).testTag("page").tabSwipe(rememberTabSeek(), Route.UPCOMING) { picked += it })
        }
        // A flick of a sixth of the width in 50 ms, and the same distance dragged over a second.
        compose.onNodeWithTag("page").performTouchInput {
            swipe(center, center.copy(x = center.x - width / 6f), durationMillis = 50)
        }
        compose.onNodeWithTag("page").performTouchInput {
            swipe(center, center.copy(x = center.x + width / 6f), durationMillis = 1000)
        }
        assertEquals(listOf(Route.CALENDAR), picked)
    }

    @Test
    fun anEmptyAreaSwitchesTabsButASwipeableRowKeepsItsSwipe() {
        val picked = mutableListOf<String>()
        compose.setContent {
            val seek = rememberTabSeek()
            Column(Modifier.size(400.dp).tabSwipe(seek, Route.UPCOMING) { picked += it }) {
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

    @Test
    fun aStretchFollowsEachStepAtOnceAndSpringsBackOnLettingGo() {
        lateinit var seek: TabSeek
        compose.setContent { seek = rememberTabSeek() }
        compose.runOnIdle {
            seek.drag(null, 0f, edge = 12f)
            assertEquals(12f, seek.stretch, 0.001f)
            seek.end()
        }
        compose.mainClock.advanceTimeBy(SETTLED_MS)
        compose.runOnIdle { assertEquals(0f, seek.stretch, 0.001f) }
    }

    @Test
    fun aNewDragTakesOverFromASpringBack() {
        lateinit var seek: TabSeek
        compose.setContent { seek = rememberTabSeek() }
        compose.runOnIdle {
            seek.drag(null, 0f, edge = 30f)
            seek.end()
        }
        compose.mainClock.advanceTimeBy(FRAME_MS * 3)
        compose.runOnIdle { seek.drag(null, 0f, edge = 10f) }
        compose.mainClock.advanceTimeBy(SETTLED_MS)
        compose.runOnIdle { assertEquals(10f, seek.stretch, 0.001f) }
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLED_MS = 2_000L
    }
}
