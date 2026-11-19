package com.aliraza.dailytodos.core.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Highlighting, exclusions and edge cases that a table row cannot express. */
class QuickAddParserTest {
    private val now = LocalDateTime.of(2026, 11, 18, 10, 30)

    private fun parse(text: String, excluded: Set<IntRange> = emptySet()) =
        QuickAddParser.parse(QuickAddInput(text, now, listNames = listOf("Groceries"), excluded = excluded))

    @Test
    fun spansCoverTheRecognisedCharacters() {
        val text = "Call bank tomorrow at 4pm p1 #groceries"
        val result = parse(text)
        val pieces = result.spans.associate { it.kind to text.substring(it.range.first, it.range.last + 1) }
        assertEquals(
            mapOf(
                SpanKind.DATE to "tomorrow",
                SpanKind.TIME to "at 4pm",
                SpanKind.PRIORITY to "p1",
                SpanKind.LIST to "#groceries",
            ),
            pieces,
        )
    }

    @Test
    fun spanLeavesOutSurroundingPunctuation() {
        val text = "Call bank (tomorrow),"
        val span = parse(text).spans.single()
        assertEquals("tomorrow", text.substring(span.range.first, span.range.last + 1))
    }

    @Test
    fun excludedRangeIsNotRecognisedAgain() {
        val text = "Read Tomorrow book p1 tomorrow"
        val first = parse("Read the tomorrow book")
        assertEquals(LocalDate.of(2026, 11, 19), first.date)

        val tapped = first.spans.single().range
        val second = parse("Read the tomorrow book", excluded = setOf(tapped))
        assertEquals("Read the tomorrow book", second.title)
        assertNull(second.date)

        // Only the tapped occurrence is excluded; a later one is still recognised.
        val later = parse(text, excluded = setOf(5..12))
        assertEquals(LocalDate.of(2026, 11, 19), later.date)
        assertEquals("Read Tomorrow book", later.title)
    }

    @Test
    fun excludingTheTimeKeepsTheDate() {
        val text = "Call bank tomorrow 4pm"
        val timeSpan = parse(text).spans.first { it.kind == SpanKind.TIME }.range
        val result = parse(text, excluded = setOf(timeSpan))
        assertEquals("Call bank 4pm", result.title)
        assertEquals(LocalDate.of(2026, 11, 19), result.date)
        assertNull(result.time)
    }

    @Test
    fun ambiguousHourRollsToTomorrowMorningLateAtNight() {
        val late = QuickAddParser.parse(QuickAddInput("Call at 4", LocalDateTime.of(2026, 11, 18, 17, 0)))
        assertEquals(LocalDate.of(2026, 11, 19), late.date)
        assertEquals(LocalTime.of(4, 0), late.time)
    }

    @Test
    fun timeOffsetCrossesMidnightAndYear() {
        val result = QuickAddParser.parse(
            QuickAddInput("Toast in 90 minutes", LocalDateTime.of(2026, 12, 31, 23, 15, 42)),
        )
        assertEquals(LocalDate.of(2027, 1, 1), result.date)
        assertEquals(LocalTime.of(0, 45), result.time)
    }

    @Test
    fun aDateAlreadyPassedThisYearMeansNextYearAcrossNewYear() {
        val result = QuickAddParser.parse(QuickAddInput("Party 31 dec", LocalDateTime.of(2027, 1, 2, 9, 0)))
        assertEquals(LocalDate.of(2027, 12, 31), result.date)
    }

    @Test
    fun wordsAloneAreATitle() {
        val result = parse("Tomorrow p1")
        assertEquals("Tomorrow p1", result.title)
        assertNull(result.date)
        assertEquals(emptyList<Span>(), result.spans)
    }

    @Test
    fun anUntitledLineReadsItsWordsAlone() {
        val text = " #groceries 25 nov"
        val result = QuickAddParser.parse(QuickAddInput(text, now, listNames = listOf("Groceries"), untitled = true))
        assertEquals("", result.title)
        assertEquals("Groceries", result.listName)
        assertEquals(LocalDate.of(2026, 11, 25), result.date)
        assertEquals(listOf("#groceries", "25 nov"), result.spans.map { text.substring(it.range) })
    }

    @Test
    fun anUntitledLineKeepsAnEmptyTitleWhenItsWordsAreSwitchedOff() {
        val text = " #groceries"
        val input = QuickAddInput(text, now, listNames = listOf("Groceries"), excluded = setOf(1..10), untitled = true)
        val result = QuickAddParser.parse(input)
        assertEquals("", result.title)
        assertNull(result.listName)
        assertEquals(emptyList<Span>(), result.spans)
    }
}
