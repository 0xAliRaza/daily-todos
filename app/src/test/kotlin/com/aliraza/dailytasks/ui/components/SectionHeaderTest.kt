package com.aliraza.dailytasks.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A header's action is a named icon button, and its count still shows beside it. */
@RunWith(AndroidJUnit4::class)
class SectionHeaderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theActionIsANamedButtonNextToTheCount() {
        var taps = 0
        compose.setContent {
            SectionHeader(
                "Overdue",
                trailing = "2",
                action = HeaderAction(AppIcons.Reschedule, "Reschedule overdue tasks") { taps++ },
            )
        }
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithContentDescription("Reschedule overdue tasks").performClick()
        assertEquals(1, taps)
    }
}
