package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The tick follows the haptics setting: on by default, silent when switched off. */
@RunWith(AndroidJUnit4::class)
class HapticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun ticks(on: Boolean): Int {
        var count = 0
        val recorder = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                count++
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recorder, LocalHapticsOn provides on) {
                rememberTick()()
            }
        }
        compose.waitForIdle()
        return count
    }

    @Test
    fun ticksWhenOn() = assertEquals(1, ticks(on = true))

    @Test
    fun staysQuietWhenOff() = assertEquals(0, ticks(on = false))
}
