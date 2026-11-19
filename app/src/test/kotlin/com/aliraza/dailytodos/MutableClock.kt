package com.aliraza.dailytodos

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A clock tests can move forward. */
class MutableClock(var now: Instant, private val zoneId: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
}
