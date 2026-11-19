package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.parse.QuickAddInput
import com.aliraza.dailytodos.core.parse.QuickAddParser
import com.aliraza.dailytodos.core.parse.SpanKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 18, 9, 0)
private val TODAY: LocalDate = NOW.toLocalDate()
private val TOMORROW: LocalDate = TODAY.plusDays(1)

private fun parse(text: String, excluded: Set<IntRange>) =
    QuickAddParser.parse(QuickAddInput(text, NOW, listNames = listOf("Daily", "Groceries"), excluded = excluded))

private fun QuickAddState.read() = read(excluded.keys, ::parse)

private fun opened(words: String): QuickAddState = QuickAddState().apply { show(words) }

/** The line the add bar starts with, from the list or day it was opened on. */
class QuickAddStartTest {

    @Test
    fun aListScreenStartsWithItsTagAndTheDefaultListWithNothing() {
        assertEquals("#groceries", startWords("Groceries", null, TODAY, null))
        assertEquals("", startWords(null, null, TODAY, TOMORROW))
    }

    @Test
    fun aPickedDayIsWrittenAsADatePickWouldWriteIt() {
        assertEquals("25 nov", startWords(null, LocalDate.of(2026, 11, 25), TODAY, TOMORROW))
        assertEquals("today", startWords(null, TODAY, TODAY, TOMORROW))
    }

    @Test
    fun theDefaultTomorrowIsLeftToThePill() {
        assertEquals("", startWords(null, TOMORROW, TODAY, TOMORROW))
        assertEquals("tomorrow", startWords(null, TOMORROW, TODAY, null))
    }

    @Test
    fun theStartWordsFollowASpaceWithTheCursorBeforeIt() {
        val state = opened("#groceries")
        assertEquals(" #groceries", state.text)
        assertEquals(TextRange(0), state.value.selection)
    }

    @Test
    fun startWordsAloneAreRecognisedButMakeNoTask() {
        val read = opened("#groceries").read()
        assertEquals("Groceries", read.listName)
        assertEquals("", read.title)
        assertEquals(listOf(1..10), read.spans.filter { it.kind == SpanKind.LIST }.map { it.range })
    }

    @Test
    fun aPickedDayAloneIsRecognised() {
        assertEquals(LocalDate.of(2026, 11, 25), opened("25 nov").read().date)
    }

    @Test
    fun aTypedTitleGoesBeforeTheStartWords() {
        val state = opened("#groceries")
        state.edit(TextFieldValue("Milk #groceries", TextRange(4)))
        val read = state.read()
        assertEquals("Milk", read.title)
        assertEquals("Groceries", read.listName)
    }

    @Test
    fun theNextLineStartsAgainAfterAnAdd() {
        val state = opened("#groceries")
        state.edit(TextFieldValue("Milk #groceries", TextRange(4)))
        state.clear()
        assertEquals(" #groceries", state.text)
        assertEquals(TextRange(0), state.value.selection)
    }

    @Test
    fun tappingAStartWordUnrecognisesIt() {
        val state = opened("#groceries")
        val spans = state.read().spans.map { it.range }
        state.edit(TextFieldValue(state.text, TextRange(5)), spans)
        assertEquals(mapOf(1..10 to "#groceries"), state.excluded)
        assertNull(state.read().listName)
    }

    @Test
    fun reopeningKeepsADraftButRefreshesUntouchedStartWords() {
        val state = opened("#groceries")
        state.show("25 nov")
        assertEquals(" 25 nov", state.text)
        state.edit(TextFieldValue("Milk 25 nov", TextRange(4)))
        state.show("#groceries")
        assertEquals("Milk 25 nov", state.text)
    }

    @Test
    fun noStartWordsLeaveTheLineEmpty() {
        val state = opened("")
        assertEquals("", state.text)
        assertEquals("Call bank", state.apply { edit(TextFieldValue("Call bank")) }.read().title)
    }
}

/** The bar as the app wires it, opened with start words. */
@RunWith(AndroidJUnit4::class)
class QuickAddStartBarTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Bar(state: QuickAddState) {
        val parsed = remember(state.text, state.excluded) { state.read() }
        val all = remember(state.text) { state.read(emptySet(), ::parse) }
        val off = state.defaultDateOff || switchedOffWhen(all, state.excluded.keys).isNotEmpty()
        val default = DefaultDate(TOMORROW, LocalTime.of(10, 0))
            .takeIf { parsed.date == null && parsed.title.isNotBlank() && parsed.listName == null }
        QuickAddBar(
            state = state,
            parsed = parsed,
            due = due(parsed, all, state.excluded.keys, default, off),
            today = TODAY,
            placeholder = "Add a task",
            options = AddOptions(listOf("Daily", "Groceries"), parsed.listName ?: "Daily"),
            onSubmit = {},
        )
    }

    @Test
    fun aListScreenShowsItsTagInTheLineAndOnTheChip() {
        compose.setContent { Bar(opened("#groceries")) }
        compose.onNode(hasSetTextAction()).assertTextEquals(" #groceries")
        compose.onNodeWithText("Groceries").assertExists()
        compose.onNodeWithContentDescription("Add task").assertDoesNotExist()
    }

    @Test
    fun typingPutsTheTitleBeforeTheStartWords() {
        val state = opened("#groceries")
        compose.setContent { Bar(state) }
        compose.onNode(hasSetTextAction()).performTextInput("Milk")
        compose.waitForIdle()
        assertEquals("Milk #groceries", state.text)
        assertEquals("Groceries", state.read().listName)
        compose.onNodeWithContentDescription("Add task").assertExists()
    }

    @Test
    fun theDefaultListKeepsTheTomorrowPill() {
        val state = opened(startWords(null, TOMORROW, TODAY, TOMORROW))
        compose.setContent { Bar(state) }
        compose.onNode(hasSetTextAction()).performTextInput("Call bank")
        compose.waitForIdle()
        assertEquals("Call bank", state.text)
        compose.onNodeWithText("Tomorrow").assertExists()
        assertNull(state.read().date)
    }
}
