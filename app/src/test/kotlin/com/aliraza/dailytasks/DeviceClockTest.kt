package com.aliraza.dailytasks

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.TimeZone

class DeviceClockTest {
    private val original = TimeZone.getDefault()

    @After
    fun restore() = TimeZone.setDefault(original)

    @Test
    fun followsTheTimeZoneWhileTheProcessRuns() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"))
        assertEquals(ZoneId.of("Asia/Karachi"), DeviceClock.zone)
        // Android resets the process default when the phone's zone changes.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        assertEquals(ZoneId.of("Asia/Kolkata"), DeviceClock.zone)
    }

    /** Tripwire: a clock or zone read once would freeze the zone again. */
    @Test
    fun noOtherCodeReadsTheSystemZone() {
        val sources = listOf("src/main", "../core/src/main").map(::File)
            .flatMap { root -> root.walk().filter { it.extension == "kt" }.toList() }
        assertTrue("no sources found", sources.isNotEmpty())
        val offenders = sources
            .filter { it.name != "DeviceClock.kt" }
            .filter { file -> FORBIDDEN.any { it in file.readText() } }
            .map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }

    private companion object {
        val FORBIDDEN = listOf("systemDefaultZone(", "ZoneId.systemDefault(", "TimeZone.getDefault(")
    }
}
