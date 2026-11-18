package com.aliraza.dailytasks.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.aliraza.dailytasks.core.parse.ParseResult
import com.aliraza.dailytasks.core.parse.Span
import com.aliraza.dailytasks.core.parse.SpanKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickAddStateTest {

    private val line = "Call bank tomorrow"
    private val tomorrow = 10..17

    private fun typed(): QuickAddState = QuickAddState().apply { edit(TextFieldValue(line, TextRange(line.length))) }

    @Test
    fun tappingInsideARecognisedWordUnrecognisesIt() {
        val state = typed()
        state.edit(TextFieldValue(line, TextRange(13)), listOf(tomorrow))
        assertEquals(mapOf(tomorrow to "tomorrow"), state.excluded)
    }

    @Test
    fun typingNeverUnrecognises() {
        val state = QuickAddState()
        line.indices.forEach { end ->
            state.edit(TextFieldValue(line.take(end + 1), TextRange(end + 1)), listOf(tomorrow))
        }
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun placingTheCursorAtAWordsEdgeKeepsIt() {
        val state = typed()
        state.edit(TextFieldValue(line, TextRange(tomorrow.first)), listOf(tomorrow))
        state.edit(TextFieldValue(line, TextRange(tomorrow.last + 1)), listOf(tomorrow))
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun selectingARangeKeepsIt() {
        val state = typed()
        state.edit(TextFieldValue(line, TextRange(11, 15)), listOf(tomorrow))
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun tappingAnUnrecognisedWordBringsItBack() {
        val state = typed()
        state.edit(TextFieldValue(line, TextRange(13)), listOf(tomorrow))
        assertEquals(TextRange(tomorrow.last + 1), state.value.selection)
        state.edit(TextFieldValue(line, TextRange(13)), emptyList())
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun aDateAndItsTimeSwitchTogether() {
        val text = "Call bank tuesday 4pm"
        val day = 10..16
        val time = 18..20
        val state = QuickAddState().apply { edit(TextFieldValue(text, TextRange(text.length))) }
        state.edit(TextFieldValue(text, TextRange(19)), listOf(day, time), tied = listOf(day, time))
        assertEquals(setOf(day, time), state.excluded.keys)
        state.edit(TextFieldValue(text, TextRange(13)), emptyList(), tied = listOf(day, time))
        assertTrue(state.excluded.isEmpty())
    }

    @Test
    fun theBarClosesWhenItLosesTheKeyboardButNotBeforeItHadIt() {
        val state = QuickAddState()
        state.show()
        state.focusChanged(false)
        assertTrue(state.open)
        state.focusChanged(true)
        state.edit(TextFieldValue("Milk", TextRange(4)))
        state.focusChanged(false)
        assertFalse(state.open)
        assertEquals("Milk", state.text)
    }

    @Test
    fun anExclusionLapsesWhenItsWordIsEdited() {
        val state = typed()
        state.exclude(tomorrow)
        state.edit(TextFieldValue("Call bank tomorrw", TextRange(17)))
        assertTrue(state.excluded.isEmpty())
    }
}

class DuePillTest {

    private val friday = LocalDate.of(2026, 11, 20)
    private val tomorrow = LocalDate.of(2026, 11, 19)
    private val word = 10..15
    private val read = ParseResult("Call bank", date = friday, spans = listOf(Span(SpanKind.DATE, word)))
    private val plain = ParseResult("Call bank friday")
    private val default = DefaultDate(tomorrow, LocalTime.of(10, 0))

    @Test
    fun aTypedDateWinsOverTheDefault() {
        assertEquals(Due(friday, null, on = true, words = listOf(word)), due(read, read, emptySet(), default, false))
    }

    @Test
    fun aTypedDateSwitchedOffStaysShownGreyedAndKeepsTheDefaultOut() {
        val excluded = setOf(word)
        assertEquals(listOf(word), switchedOffWhen(read, excluded))
        assertEquals(Due(friday, null, on = false, words = listOf(word)), due(plain, read, excluded, default, false))
    }

    @Test
    fun theDefaultDateStaysShownWhenSwitchedOff() {
        assertEquals(
            Due(tomorrow, default.time, on = true, words = emptyList()),
            due(plain, plain, emptySet(), default, false),
        )
        assertEquals(
            Due(tomorrow, default.time, on = false, words = emptyList()),
            due(plain, plain, emptySet(), default, true),
        )
    }

    @Test
    fun noDateAndNoDefaultShowsNoPill() {
        assertNull(due(plain, plain, emptySet(), null, false))
    }

    @Test
    fun togglingThePillSwitchesItsDateOffAndBackOn() {
        val state = QuickAddState().apply { edit(TextFieldValue("Call bank friday", TextRange(16))) }
        state.toggle(Due(friday, null, on = true, words = listOf(word)))
        assertEquals(setOf(word), state.excluded.keys)
        state.toggle(Due(friday, null, on = false, words = listOf(word)))
        assertTrue(state.excluded.isEmpty())
        state.toggle(Due(tomorrow, null, on = true, words = emptyList()))
        assertTrue(state.defaultDateOff)
        state.toggle(Due(tomorrow, null, on = false, words = emptyList()))
        assertFalse(state.defaultDateOff)
    }
}
