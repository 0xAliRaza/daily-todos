package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** The one month grid, shared by the date sheet and the calendar's Month view. */
@RunWith(AndroidJUnit4::class)
class MonthGridTest {

    @get:Rule
    val compose = createComposeRule()

    private val november = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 21)

    @Test
    fun theWeeksFollowTheWeekStart() {
        // 1 November 2026 is a Sunday.
        val monday = monthDays(november, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 26), monday.first())
        assertEquals(LocalDate.of(2026, 12, 6), monday.last())
        val sunday = monthDays(november, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), sunday.first())
        assertEquals(LocalDate.of(2026, 12, 5), sunday.last())
        val saturday = monthDays(november, DayOfWeek.SATURDAY)
        assertEquals(DayOfWeek.SATURDAY, saturday.first().dayOfWeek)
        assertEquals(0, saturday.size % 7)
    }

    @Test
    fun thePickedDayWinsOverTodayAndPastDaysAreQuieter() {
        assertEquals(DayLook.PICKED, DayLook.of(today, picked = today, today = today))
        assertEquals(DayLook.TODAY, DayLook.of(today, picked = today.plusDays(6), today = today))
        assertEquals(DayLook.PAST, DayLook.of(today.minusDays(1), picked = null, today = today))
        assertEquals(DayLook.PLAIN, DayLook.of(today.plusDays(1), picked = null, today = today))
    }

    @Test
    fun theLettersStartOnTheAppsWeekStart() {
        compose.setContent {
            CompositionLocalProvider(LocalWeekStart provides DayOfWeek.WEDNESDAY) {
                MonthGrid(november, picked = null, today = today, onPick = {})
            }
        }
        // Wednesday first: W T F S S M T, so the week's only W is the first letter.
        compose.onAllNodesWithText("W").assertCountEquals(1)
        compose.onAllNodesWithText("S").assertCountEquals(2)
    }

    @Test
    fun onlyTheMonthsOwnDaysAreShownAndTapping() {
        var picked: LocalDate? = null
        compose.setContent { MonthGrid(november, picked = null, today = today, onPick = { picked = it }) }
        // 26 October sits in the first week but is left blank.
        compose.onAllNodesWithText("26").assertCountEquals(1)
        compose.onNodeWithText("31").assertDoesNotExist()
        compose.onNodeWithContentDescription(longDateLabel(LocalDate.of(2026, 11, 27))).performClick()
        assertEquals(LocalDate.of(2026, 11, 27), picked)
    }
}
