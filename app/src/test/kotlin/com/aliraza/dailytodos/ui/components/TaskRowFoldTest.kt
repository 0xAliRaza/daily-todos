package com.aliraza.dailytodos.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** The "3/4" at a row's right folds its sub-tasks in place; it must never open the task instead. */
@RunWith(AndroidJUnit4::class)
class TaskRowFoldTest {

    @get:Rule
    val compose = createComposeRule()

    private val task = Task(
        id = "t",
        listId = "l",
        title = "Trip",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun tappingTheCountFoldsTheSubtasksOpenAndShutWithoutOpeningTheTask() {
        var opened = 0
        var open by mutableStateOf(false)
        compose.setContent {
            TaskRow(
                task = task,
                today = LocalDate.of(2026, 11, 18),
                meta = RowMeta(subtasks = 3 to 4),
                selected = false,
                onToggle = {},
                onClick = { opened++ },
                onLongClick = {},
                fold = SubtaskFold(open, onToggle = { open = !open }) { Text("Pack") },
            )
        }
        compose.onNodeWithText("Pack").assertDoesNotExist()

        compose.onNodeWithContentDescription("Show sub-tasks", substring = true).performClick()
        compose.onNodeWithText("Pack").assertExists()

        compose.onNodeWithContentDescription("Hide sub-tasks", substring = true).performClick()
        compose.onNodeWithText("Pack").assertDoesNotExist()
        assertEquals(0, opened)

        compose.onNodeWithText("Trip").performClick()
        assertEquals(1, opened)
    }
}
