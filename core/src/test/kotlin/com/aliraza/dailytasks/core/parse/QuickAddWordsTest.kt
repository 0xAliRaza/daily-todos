package com.aliraza.dailytasks.core.parse

import com.aliraza.dailytasks.core.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Whatever the add bar writes for a picked value, the parser reads back as that same value. */
class QuickAddWordsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val now = LocalDateTime.of(today, LocalTime.of(9, 0))

    private fun parse(line: String, lists: List<String> = emptyList()) =
        QuickAddParser.parse(QuickAddInput(line, now, listNames = lists))

    @Test
    fun everyPickedDayReadsBackAsThatDay() {
        for (offset in -40L..800L) {
            val day = today.plusDays(offset)
            val result = parse("Dentist " + QuickAddWords.date(day, today))
            assertEquals("offset $offset", day, result.date)
            assertEquals("offset $offset", "Dentist", result.title)
        }
    }

    @Test
    fun aKeptTimeReadsBackWithThePickedDay() {
        val friday = today.plusDays(2)
        val result = parse(
            "Call bank " + QuickAddWords.date(friday, today) + " " + QuickAddWords.time(LocalTime.of(16, 30)),
        )
        assertEquals(friday, result.date)
        assertEquals(LocalTime.of(16, 30), result.time)
        assertEquals("Call bank", result.title)
    }

    @Test
    fun pickedPrioritiesAndListsReadBack() {
        Priority.entries.filter { it != Priority.NONE }.forEach { level ->
            assertEquals(level, parse("Call " + QuickAddWords.priority(level)).priority)
        }
        val lists = listOf("Weekend trip", "Groceries")
        assertEquals("Weekend trip", parse("Pack " + QuickAddWords.list("Weekend trip"), lists).listName)
        assertEquals(null, QuickAddWords.priority(Priority.NONE))
    }

    @Test
    fun aListNameWithPunctuationStillReadsBackAsItsTag() {
        val named = listOf("Ideas!", "Books (2026)", "Mom's stuff")
        named.forEach { name ->
            assertEquals(name, parse("Note " + QuickAddWords.list(name), named).listName)
        }
        assertEquals(null, QuickAddWords.list("!!!"))
    }
}
