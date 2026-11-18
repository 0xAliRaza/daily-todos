package com.aliraza.dailytasks

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The system clock in the phone's current time zone, read on every call.
 *
 * `Clock.systemDefaultZone()` fixes the zone when it is created, so a running process would keep
 * computing reminder times in the old zone after the user travels or changes it. Android resets the
 * process default zone on a change, and this clock picks that up.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
