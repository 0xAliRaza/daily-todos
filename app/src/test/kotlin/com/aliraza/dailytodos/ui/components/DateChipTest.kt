package com.aliraza.dailytodos.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.parse.QuickAddInput
import com.aliraza.dailytodos.core.parse.QuickAddParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** The add bar's Date chip shows the date and time read from the line, so a misread is one tap away. */
@RunWith(AndroidJUnit4::class)
class DateChipTest {

    @get:Rule
    val compose = createComposeRule()

    // Saturday 21 November 2026.
    private val now = LocalDateTime.of(2026, 11, 21, 9, 0)
    private val today = now.toLocalDate()
    private val tomorrowAtTen = DefaultDate(today.plusDays(1), LocalTime.of(10, 0))

    private fun parse(text: String, excluded: Set<IntRange> = emptySet()) =
        QuickAddParser.parse(QuickAddInput(text, now, listNames = listOf("Daily", "Work"), excluded = excluded))

    private fun dueOf(text: String, excluded: Set<IntRange> = emptySet()): Due? {
        val parsed = parse(text, excluded)
        val default = tomorrowAtTen.takeIf { parsed.date == null }
        return due(parsed, parse(text), excluded, default, defaultOff = false)
    }

    private fun label(text: String, excluded: Set<IntRange> = emptySet()) =
        dateChipLabel(dueOf(text, excluded), today, ClockFormat.H12, Locale.US)

    @Test
    fun aTypedDateAndTimeShowAsTheyWereRead() {
        assertEquals("Fri 27 Nov, 8:00 PM", label("Book flights friday 8pm p2 #work"))
    }

    @Test
    fun aTypedDateAloneShowsTheDay() {
        assertEquals("Fri 27 Nov", label("Book flights friday"))
    }

    @Test
    fun theClockFormatIsFollowed() {
        val label = dateChipLabel(dueOf("Book flights friday 8pm"), today, ClockFormat.H24, Locale.US)
        assertEquals("Fri 27 Nov, 20:00", label)
    }

    @Test
    fun theDefaultDateLeavesTheChipToItsOwnChip() {
        assertNull(label("Book flights"))
    }

    @Test
    fun aDateSwitchedOffLeavesTheChipEmpty() {
        val line = "Book flights friday"
        val friday = line.indexOf("friday").let { it until it + "friday".length }
        assertNull(label(line, setOf(friday)))
    }

    @Test
    fun theBarShowsTheReadDateOnItsChip() {
        val line = "Book flights friday 8pm"
        val state = QuickAddState().apply {
            show()
            edit(TextFieldValue(line, TextRange(line.length)))
        }
        compose.setContent {
            QuickAddBar(
                state = state,
                parsed = parse(state.text),
                due = dueOf(state.text),
                today = today,
                placeholder = "Add a task",
                options = AddOptions(listOf("Daily"), "Daily"),
                onSubmit = {},
            )
        }
        compose.onNodeWithText("Fri 27 Nov, 8:00 PM").assertExists()
    }
}
