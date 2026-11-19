package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.parse.QuickAddInput
import com.aliraza.dailytodos.core.parse.QuickAddParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** "No date" in the add bar's date sheet leaves the task undated, whether a date was typed or defaulted. */
@RunWith(AndroidJUnit4::class)
class QuickAddNoDateTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = LocalDateTime.of(2026, 11, 18, 9, 0)
    private val today = now.toLocalDate()

    private fun parse(text: String, excluded: Set<IntRange>) =
        QuickAddParser.parse(QuickAddInput(text, now, listNames = listOf("Daily"), excluded = excluded))

    /** The bar as the app wires it, with tomorrow at ten as the default date of an undated task. */
    @Composable
    private fun Bar(state: QuickAddState) {
        val parsed = remember(state.text, state.excluded) { parse(state.text, state.excluded.keys) }
        val all = remember(state.text) { parse(state.text, emptySet()) }
        val off = state.defaultDateOff || switchedOffWhen(all, state.excluded.keys).isNotEmpty()
        val default = DefaultDate(today.plusDays(1), LocalTime.of(10, 0))
            .takeIf { parsed.date == null && parsed.title.isNotBlank() }
        QuickAddBar(
            state = state,
            parsed = parsed,
            due = due(parsed, all, state.excluded.keys, default, off),
            today = today,
            placeholder = "Add a task",
            options = AddOptions(listOf("Daily"), "Daily"),
            onSubmit = {},
        )
    }

    private fun typed(line: String): QuickAddState = QuickAddState().apply {
        show()
        edit(TextFieldValue(line, TextRange(line.length)))
    }

    private fun pickNoDate(state: QuickAddState) {
        compose.setContent { Bar(state) }
        compose.onNodeWithText("Date").performClick()
        compose.onNodeWithText("Due date").assertExists()
        compose.onNodeWithText("No date").performClick()
        compose.waitForIdle()
    }

    @Test
    fun noDateRemovesATypedDateAndKeepsTheDefaultOut() {
        val state = typed("Call bank tomorrow 4pm p1")
        pickNoDate(state)
        assertEquals("Call bank p1", state.text.trim())
        assertTrue(state.defaultDateOff)
        val parsed = parse(state.text, emptySet())
        assertNull(parsed.date)
        assertNull(parsed.time)
        assertFalse(state.picking)
    }

    @Test
    fun noDateTurnsTheDefaultPillOff() {
        val state = typed("Call bank")
        pickNoDate(state)
        assertEquals("Call bank", state.text)
        assertTrue(state.defaultDateOff)
        assertFalse(state.picking)
    }

    @Test
    fun pickingADayWritesItIntoTheLine() {
        val state = typed("Call bank")
        compose.setContent { Bar(state) }
        compose.onNodeWithText("Date").performClick()
        compose.onNodeWithText("Today").performClick()
        compose.waitForIdle()
        assertEquals(LocalDate.of(2026, 11, 18), parse(state.text, emptySet()).date)
        assertFalse(state.defaultDateOff)
    }
}
