package com.aliraza.dailytasks.core.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class FunFactsTest {
    private val day = LocalDate.of(2026, 11, 19)

    @Test
    fun theCountAlwaysReadsWell() {
        val totals = listOf(Duration.ofSeconds(10), Duration.ofMinutes(25), Duration.ofHours(40), Duration.ofDays(400))
        totals.forEach { total ->
            (0L until 30).forEach { offset ->
                val picked = FunFacts.pick(total, day.plusDays(offset))!!
                assertTrue("$total $picked", picked.count in FunFacts.COUNTS)
                assertEquals(total.toMillis() / picked.fact.length.toMillis(), picked.count)
            }
        }
    }

    @Test
    fun theSameAllDayAndADifferentOneOverDays() {
        val total = Duration.ofHours(10)
        assertEquals(FunFacts.pick(total, day), FunFacts.pick(total, day))
        val seen = (0L until 10).mapNotNull { FunFacts.pick(total, day.plusDays(it))?.fact }.toSet()
        assertTrue(seen.toString(), seen.size > 1)
    }

    @Test
    fun aFewMinutesCompareToSunlight() {
        val picks = (0L until 10).mapNotNull { FunFacts.pick(Duration.ofMinutes(25), day.plusDays(it)) }.toSet()
        assertTrue(picks.toString(), FunComparison(FunFact.SUNLIGHT, 3) in picks)
    }

    @Test
    fun nothingToCompareBelowTheShortestFact() {
        assertNull(FunFacts.pick(Duration.ofSeconds(2), day))
        assertNull(FunFacts.pick(Duration.ZERO, day))
    }
}
