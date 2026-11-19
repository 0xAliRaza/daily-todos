package com.aliraza.dailytodos.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The dial sets the length by touch while it can change, and stays still while a round runs. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class DialInputDragTest {
    @get:Rule
    val compose = createComposeRule()

    private var minutes by mutableIntStateOf(25)
    private var settled by mutableStateOf(true)
    private var ticks = 0

    private fun showDial() {
        val recorder = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                ticks++
            }
        }
        compose.setContent {
            val length = LengthPicker(minutes, 1..180, 5, emptyList(), "Focus") { minutes = it }
            CompositionLocalProvider(LocalHapticFeedback provides recorder) {
                Box(Modifier.size(240.dp).testTag("dial").dialInput(rememberDialInput(length.takeIf { settled })))
            }
        }
    }

    @Test
    fun aDragRoundTheRingSetsTheLengthAndWindsPastTwelve() {
        showDial()
        compose.onNodeWithTag("dial").performTouchInput {
            down(topCenter)
            moveTo(centerRight)
            moveTo(bottomCenter)
            moveTo(centerLeft)
            moveTo(topCenter)
            moveTo(centerRight)
            up()
        }
        compose.waitForIdle()
        assertEquals(75, minutes)
        // The touch jumps from 25 down to 1 past the 5s, then winds by quarters: 15, 30, 45, 60 and 75.
        assertEquals(6, ticks)
    }

    @Test
    fun aTapOnTheRingJumpsToItsMinute() {
        showDial()
        compose.onNodeWithTag("dial").performTouchInput {
            down(bottomCenter.copy(y = bottomCenter.y - 1f))
            up()
        }
        compose.waitForIdle()
        assertEquals(30, minutes)
    }

    @Test
    fun theTimeInTheMiddleIsNotATouchTarget() {
        showDial()
        compose.onNodeWithTag("dial").performTouchInput {
            down(center)
            moveTo(centerRight)
            up()
        }
        compose.waitForIdle()
        assertEquals(25, minutes)
    }

    @Test
    fun aRunningRoundKeepsTheDialStill() {
        settled = false
        showDial()
        compose.onNodeWithTag("dial").performTouchInput {
            down(topCenter)
            moveTo(centerRight)
            up()
        }
        compose.waitForIdle()
        assertEquals(25, minutes)
        compose.onNodeWithTag("dial").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun screenReadersCanSetTheLength() {
        showDial()
        compose.onNodeWithTag("dial").performSemanticsAction(SemanticsActions.SetProgress) { it(40.4f) }
        compose.waitForIdle()
        assertEquals(40, minutes)
    }
}
