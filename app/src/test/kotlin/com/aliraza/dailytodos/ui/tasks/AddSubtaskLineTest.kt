package com.aliraza.dailytodos.ui.tasks

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sub-tasks typed under a task are added one after another without leaving the line. */
@RunWith(AndroidJUnit4::class)
class AddSubtaskLineTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun enterAddsAndKeepsTheFieldReadyForTheNextOne() {
        val added = mutableListOf<String>()
        compose.setContent { AddSubtaskLine { added += it } }
        compose.onNodeWithText("Add a sub-task").performClick()
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Hamstrings ")
        field.performImeAction()
        field.performTextInput("  ")
        field.performImeAction()
        field.performTextInput("Calves")
        field.performImeAction()
        assertEquals(listOf("Hamstrings", "Calves"), added)
    }
}
