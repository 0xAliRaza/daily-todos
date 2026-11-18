package com.aliraza.dailytasks.core.model

import java.time.Instant

/** How a reminder reached the user. */
enum class FiredBy { ALARM, CATCH_UP, BEFORE_UNLOCK }

/** One posted reminder in the history: when it was due and when it actually fired. Holds no title. */
data class ReminderLogEntry(val taskId: String, val dueAt: Instant, val firedAt: Instant, val firedBy: FiredBy)
