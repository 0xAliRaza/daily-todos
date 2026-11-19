package com.aliraza.dailytodos.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalTime

/** The time picker's panel, on a phone-sized screen so its buttons sit below the clock face. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class TimeDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aSetTimeCanBeRemovedFromTheClock() {
        var cleared = false
        var closed = false
        compose.setContent {
            TimePanel(LocalTime.of(16, 0), onDismiss = { closed = true }, onPick = {}, onClear = { cleared = true })
        }
        compose.onNodeWithText("Remove time").performClick()
        assertTrue(cleared && closed)
    }

    @Test
    fun okPicksTheShownTimeAndThereIsNothingToRemoveWithoutOne() {
        var picked: LocalTime? = null
        compose.setContent { TimePanel(LocalTime.of(9, 30), onDismiss = {}, onPick = { picked = it }, onClear = null) }
        compose.onNodeWithText("Remove time").assertDoesNotExist()
        compose.onNodeWithText("OK").performClick()
        assertEquals(LocalTime.of(9, 30), picked)
    }

    @Test
    fun theKeyboardTogglesToTypedInput() {
        compose.setContent { TimePanel(LocalTime.of(9, 30), onDismiss = {}, onPick = {}, onClear = null) }
        compose.onNodeWithContentDescription("Type the time").performClick()
        compose.onNodeWithContentDescription("Use the clock").assertExists()
    }
}
