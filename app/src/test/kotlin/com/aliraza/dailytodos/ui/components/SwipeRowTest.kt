package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A swiped row never stays half open: a short, slow swipe springs back; a quick flick acts. */
@RunWith(AndroidJUnit4::class)
class SwipeRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<SwipeAction>()

    private fun row() = compose.setContent {
        SwipeRow(
            enabled = true,
            canFocus = true,
            onFocus = { actions += SwipeAction.FOCUS },
            onDelete = { actions += SwipeAction.DELETE },
        ) { Box(Modifier.fillMaxWidth().height(56.dp).testTag("content")) }
    }

    /** Drags by [dx] in [steps] moves of 16 ms each, then lets go. */
    private fun drag(dx: Float, steps: Int) = compose.onNodeWithTag("content").performTouchInput {
        down(center)
        repeat(steps) { moveBy(Offset(dx / steps, 0f), delayMillis = 16) }
        repeat(STILL_FRAMES) { moveBy(Offset.Zero, delayMillis = 16) }
        up()
    }

    private fun settledLeft(): Float {
        compose.waitForIdle()
        return compose.onNodeWithTag("content").getBoundsInRoot().left.value
    }

    @Test
    fun aShortSlowSwipeSpringsAllTheWayBack() {
        row()
        drag(dx = 60f, steps = 30)
        assertEquals(0f, settledLeft(), 0.5f)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun aShortSlowSwipeLeftSpringsBackToo() {
        row()
        drag(dx = -60f, steps = 30)
        assertEquals(0f, settledLeft(), 0.5f)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun aQuickFlickActsWithoutReachingTheThreshold() {
        row()
        compose.onNodeWithTag("content").performTouchInput {
            down(center)
            repeat(4) { moveBy(Offset(20f, 0f), delayMillis = 8) }
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(SwipeAction.FOCUS), actions)
        assertEquals(0f, settledLeft(), 0.5f)
    }

    private companion object {
        /** Holding still before letting go, so the swipe ends slow. */
        const val STILL_FRAMES = 6
    }
}
