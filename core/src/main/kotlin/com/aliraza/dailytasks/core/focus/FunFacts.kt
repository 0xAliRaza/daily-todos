package com.aliraza.dailytasks.core.focus

import java.time.Duration
import java.time.LocalDate

/**
 * Things that take a known time, to put a focus total in proportion ("that's 72 trips of sunlight to
 * Earth"). Bundled with the app, so nothing is fetched. Each figure is rounded and its source named.
 */
enum class FunFact(val length: Duration) {
    /** Laser light to the Moon and back, about 2.5 s on average (NASA, lunar laser ranging). */
    MOON_LASER(Duration.ofMillis(2_500)),

    /** Sunlight reaching Earth, about 8 min 20 s on average (Smithsonian, "Ask an Explainer"). */
    SUNLIGHT(Duration.ofSeconds(500)),

    /** One orbit of the International Space Station, 90 to 93 min (NASA JSC orbit tutorial). */
    ISS_ORBIT(Duration.ofMinutes(92)),

    /** The adult life of a mayfly, from 30 minutes to a day or two by species (New World Encyclopedia). */
    MAYFLY(Duration.ofDays(1)),

    /** Apollo 11 from launch to landing on the Moon, 102 h 45 min (NASA Kennedy Space Center). */
    APOLLO_11(Duration.ofMinutes(102L * 60 + 45)),

    /** One orbit of the Moon around Earth, 27 d 7 h 43 min, a sidereal month (NASA StarChild). */
    MOON_ORBIT(Duration.ofMinutes(27L * 24 * 60 + 7 * 60 + 43)),
}

/** A fact and how many times it fits into the focus total. */
data class FunComparison(val fact: FunFact, val count: Long)

object FunFacts {
    /** Counts outside this range read badly: "0 trips" says nothing, "48,000 trips" is just a number. */
    val COUNTS = 1L..999L

    /**
     * The comparison to show for [total] on [day]: among the facts that fit [COUNTS] times, a different
     * one each day, the same all day. Null when none fits, which only happens below a few seconds.
     */
    fun pick(total: Duration, day: LocalDate): FunComparison? {
        val fitting = FunFact.entries
            .map { FunComparison(it, total.toMillis() / it.length.toMillis()) }
            .filter { it.count in COUNTS }
        if (fitting.isEmpty()) return null
        return fitting[Math.floorMod(day.toEpochDay(), fitting.size.toLong()).toInt()]
    }
}
