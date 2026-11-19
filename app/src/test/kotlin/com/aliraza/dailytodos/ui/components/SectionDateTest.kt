package com.aliraza.dailytodos.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class SectionDateTest {
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun aDayThisYearLeavesTheYearOut() {
        assertEquals("Fri 20 Nov", Locale.ENGLISH.use { sectionDate(LocalDate.of(2026, 11, 20), today) })
    }

    @Test
    fun aDayInAnotherYearShowsIt() {
        assertEquals("Mon 15 Nov 2027", Locale.ENGLISH.use { sectionDate(LocalDate.of(2027, 11, 15), today) })
    }

    private fun <T> Locale.use(block: () -> T): T {
        val before = Locale.getDefault()
        Locale.setDefault(this)
        return try {
            block()
        } finally {
            Locale.setDefault(before)
        }
    }
}
