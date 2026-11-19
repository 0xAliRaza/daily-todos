package com.aliraza.dailytodos.ui.focus

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The window keeps the screen on only while asked to, and the timer's cup flips the one setting. */
@RunWith(AndroidJUnit4::class)
class KeepScreenOnTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theWindowFlagFollowsTheValueAndClearsWhenLeft() {
        var on by mutableStateOf(true)
        var shown by mutableStateOf(true)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            if (shown) KeepScreenOn(on)
        }
        compose.waitForIdle()
        assertTrue(view.keepScreenOn)

        on = false
        compose.waitForIdle()
        assertFalse(view.keepScreenOn)

        on = true
        compose.waitForIdle()
        assertTrue(view.keepScreenOn)

        shown = false
        compose.waitForIdle()
        assertFalse(view.keepScreenOn)
    }

    @Test
    fun theToggleIsASwitchThatFlipsTheSetting() {
        var setting by mutableStateOf(false)
        val picks = mutableListOf<Boolean>()
        compose.setContent {
            KeepScreenOnToggle(setting, onChange = {
                picks += it
                setting = it
            })
        }
        val toggle = compose.onNodeWithContentDescription("Keep screen on")
        toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOff()

        toggle.performClick()
        toggle.assertIsOn()
        toggle.performClick()
        toggle.assertIsOff()
        assertEquals(listOf(true, false), picks)
    }
}
