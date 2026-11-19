package com.aliraza.dailytodos.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class LatenessTest {
    private val due = Instant.parse("2026-11-19T09:00:00Z")

    @Test
    fun aReminderWithinAMinuteOfItsTimeIsOnTime() {
        assertEquals(Lateness.OnTime, lateness(due, due))
        assertEquals(Lateness.OnTime, lateness(due, due.plusSeconds(60)))
    }

    @Test
    fun aReminderThatFiredEarlyCountsAsOnTime() {
        assertEquals(Lateness.OnTime, lateness(due, due.minusSeconds(300)))
    }

    @Test
    fun pastAMinuteItIsLateByTheWholeGap() {
        assertEquals(Lateness.Late(Duration.ofSeconds(61)), lateness(due, due.plusSeconds(61)))
        val gap = Duration.ofHours(3).plusMinutes(42)
        assertEquals(Lateness.Late(gap), lateness(due, due.plus(gap)))
    }
}
