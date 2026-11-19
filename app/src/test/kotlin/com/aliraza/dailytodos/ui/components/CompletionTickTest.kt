package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Ticking a task's box off confirms it with one tick; clearing it, or haptics off, stays quiet. */
@RunWith(AndroidJUnit4::class)
class CompletionTickTest {
    @get:Rule
    val compose = createComposeRule()

    private fun ticksAfterTap(checked: Boolean, hapticsOn: Boolean = true): List<HapticFeedbackType> {
        val ticks = mutableListOf<HapticFeedbackType>()
        val recorder = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                ticks += hapticFeedbackType
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recorder, LocalHapticsOn provides hapticsOn) {
                RoundCheck(checked = checked, onClick = {})
            }
        }
        compose.onNodeWithContentDescription(if (checked) "Mark not done" else "Complete").performClick()
        return ticks
    }

    @Test
    fun completingTicksOnce() = assertEquals(listOf(HapticFeedbackType.Confirm), ticksAfterTap(checked = false))

    @Test
    fun reopeningStaysQuiet() = assertEquals(emptyList<HapticFeedbackType>(), ticksAfterTap(checked = true))

    @Test
    fun hapticsOffStaysQuiet() =
        assertEquals(emptyList<HapticFeedbackType>(), ticksAfterTap(checked = false, hapticsOn = false))
}
