package com.aliraza.dailytodos.core.parse

import com.aliraza.dailytodos.core.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The README's "How to use" examples, each still in the README and still read the way it says. */
class ReadmeExamplesTest {
    // Wednesday, so "friday" is two days on.
    private val now = LocalDateTime.of(2026, 11, 18, 10, 30)
    private val readme = File("../README.md").readText()

    private fun parse(text: String): ParseResult {
        assertTrue("README no longer shows `$text`", readme.contains("`$text`"))
        return QuickAddParser.parse(QuickAddInput(text, now, listNames = listOf("Groceries")))
    }

    @Test
    fun callBank() {
        val result = parse("Call bank tomorrow 4pm p1")
        assertEquals(
            ParseResult("Call bank", LocalDate.of(2026, 11, 19), LocalTime.of(16, 0), Priority.P1),
            result.copy(spans = emptyList()),
        )
    }

    @Test
    fun dentist() {
        val result = parse("Dentist friday 1030a")
        assertEquals(LocalDate.of(2026, 11, 20), result.date)
        assertEquals(LocalTime.of(10, 30), result.time)
    }

    @Test
    fun milk() {
        val result = parse("Milk #groceries")
        assertEquals("Groceries", result.listName)
        assertEquals(null, result.date)
    }

    @Test
    fun payRent() {
        val result = parse("Pay rent 1 dec 9am")
        assertEquals(LocalDate.of(2026, 12, 1), result.date)
        assertEquals(LocalTime.of(9, 0), result.time)
    }

    @Test
    fun planTrip() {
        val result = parse("Plan trip #weekend-trip")
        assertEquals("Weekend trip", result.listName)
        assertTrue(result.newList)
    }
}
