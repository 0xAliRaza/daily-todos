package com.aliraza.dailytasks.ui.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytasks.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** The sheet's title and notes save themselves; a save must always land on the task it was typed into. */
@RunWith(AndroidJUnit4::class)
class TextEditsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun task(id: String, title: String) =
        Task(id = id, listId = "l", title = title, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)

    private fun record(saves: MutableList<Triple<String, String, String>>) =
        { id: String, title: String, notes: String -> saves += Triple(id, title, notes) }

    @Test
    fun switchingToAnotherTaskNeverCopiesTheFirstTitleOntoIt() {
        val saves = mutableListOf<Triple<String, String, String>>()
        var shown by mutableStateOf(task("a", "Water plants"))
        var open by mutableStateOf(true)
        compose.setContent { if (open) TextEdits(shown, onSave = record(saves), onToggle = {}) }
        shown = task("b", "Pay rent")
        compose.waitForIdle()
        open = false
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        assertEquals(emptyList<Triple<String, String, String>>(), saves)
    }

    @Test
    fun textTypedIntoATaskIsSavedToItWhenTheSheetMovesOn() {
        val saves = mutableListOf<Triple<String, String, String>>()
        var shown by mutableStateOf(task("a", "Water plants"))
        compose.setContent { TextEdits(shown, onSave = record(saves), onToggle = {}) }
        compose.onNodeWithText("Water plants").performTextReplacement("Water the plants")
        compose.waitForIdle()
        shown = task("b", "Pay rent")
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        assertEquals(listOf(Triple("a", "Water the plants", "")), saves.distinct())
    }

    private companion object {
        const val SETTLE_MS = 1_000L
    }
}
