package com.aliraza.dailytodos.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.data.TaskFocusTotal
import java.time.Duration

private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L

/** Focus time to read: "25 min", "2 h", "1 h 20 min". */
@Composable
fun focusDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0L -> stringResource(R.string.focus_minutes, rest.toInt())
        rest == 0L -> stringResource(R.string.duration_hours, hours.toInt())
        else -> stringResource(R.string.duration_hours_minutes, hours.toInt(), rest.toInt())
    }
}

/** A task's focus so far: "Focused 1 h 20 min · 4 sessions", or an invitation while it has none. */
@Composable
fun focusTotal(focused: TaskFocusTotal): String = if (focused.sessions == 0) {
    stringResource(R.string.task_focus_none)
} else {
    stringResource(
        R.string.task_focus_total,
        focusDuration(focused.focused),
        pluralStringResource(R.plurals.focus_sessions, focused.sessions, focused.sessions),
    )
}

/** A countdown: "24:59". Rounds up, so the last second shows 00:01 rather than 00:00. */
fun countdown(left: Duration): String {
    val seconds = (left.toMillis() + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

private const val MILLIS_PER_SECOND = 1_000L
