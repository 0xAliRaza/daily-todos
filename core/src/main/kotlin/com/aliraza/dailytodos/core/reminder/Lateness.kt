package com.aliraza.dailytodos.core.reminder

import java.time.Duration
import java.time.Instant

/** How a fired reminder compares with its time, as Reminder health shows it. */
sealed interface Lateness {
    /** Fired within [ON_TIME] of its time, or before it. */
    data object OnTime : Lateness

    /** Fired [by] after its time. */
    data class Late(val by: Duration) : Lateness

    companion object {
        /** A reminder that fired within this long of its time counts as on time. */
        val ON_TIME: Duration = Duration.ofSeconds(60)
    }
}

/** Whether a reminder due at [due] that fired at [fired] was on time, or how late it was. */
fun lateness(due: Instant, fired: Instant): Lateness {
    val gap = Duration.between(due, fired)
    return if (gap <= Lateness.ON_TIME) Lateness.OnTime else Lateness.Late(gap)
}
