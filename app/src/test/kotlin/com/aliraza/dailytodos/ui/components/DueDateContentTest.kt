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
import java.time.LocalDate
import java.time.LocalTime

/** Picking a date in one layer: quick days apply at once, a day from the month waits for Done. */
// Tall enough that the whole view, from the quick days to the Time row, is on screen to tap.
@Config(qualifiers = "w411dp-h1100dp")
@RunWith(AndroidJUnit4::class)
class DueDateContentTest {

    @get:Rule
    val compose = createComposeRule()

    // Saturday 21 November 2026; weeks start on Monday, so next week is Monday 23 November.
    private val today = LocalDate.of(2026, 11, 21)
    private val fourPm = LocalTime.of(16, 0)
    private val picks = mutableListOf<DueChoice>()
    private var closed = 0

    private fun show(initial: DueChoice) {
        compose.setContent {
            DueDateContent(
                title = "Call bank",
                initial = initial,
                today = today,
                withTime = true,
                onPick = { picks += it },
                onClose = { closed++ },
                onBack = { closed++ },
            )
        }
    }

    @Test
    fun aQuickDayAppliesAtOnceAndKeepsTheTime() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithText("Next week").performClick()
        assertEquals(listOf(DueChoice(LocalDate.of(2026, 11, 23), fourPm)), picks)
        assertEquals(1, closed)
    }

    @Test
    fun noDateClearsTheDateAndTheTime() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithText("No date").performClick()
        assertEquals(listOf(DueChoice(null, null)), picks)
    }

    @Test
    fun aDayFromTheMonthWaitsForDone() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithContentDescription(longDateLabel(LocalDate.of(2026, 11, 27))).performClick()
        assertTrue(picks.isEmpty())
        compose.onNodeWithText("Fri 27 Nov").assertExists()
        compose.onNodeWithText("Done").performClick()
        assertEquals(listOf(DueChoice(LocalDate.of(2026, 11, 27), fourPm)), picks)
    }

    @Test
    fun backDropsTheDraft() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithContentDescription(longDateLabel(LocalDate.of(2026, 11, 27))).performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        assertTrue(picks.isEmpty())
        assertEquals(1, closed)
    }

    @Test
    fun doneWithoutAChangeChangesNothing() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithText("Done").performClick()
        assertTrue(picks.isEmpty())
        assertEquals(1, closed)
    }

    @Test
    fun clearingTheTimeKeepsTheDay() {
        show(DueChoice(today, fourPm))
        compose.onNodeWithContentDescription("Clear the time").performClick()
        compose.onNodeWithText("Add time").assertExists()
        compose.onNodeWithText("Done").performClick()
        assertEquals(listOf(DueChoice(today, null)), picks)
    }

    @Test
    fun aTimeOnAnUndatedChoicePutsItOnToday() {
        assertEquals(DueChoice(today, fourPm), DueChoice(null, null).withTime(fourPm, today))
        assertEquals(DueChoice(null, null), DueChoice(null, null).withTime(null, today))
    }
}
