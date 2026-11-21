package com.aliraza.dailytodos.reminder

import android.content.Context
import androidx.core.content.edit
import com.aliraza.dailytodos.core.model.FiredBy
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Sends the Reminder health screen's test reminder. */
fun interface TestReminder {
    suspend fun send()

    companion object {
        /**
         * The reserved id the test reminder uses where a task id goes: the notification's tag, its buttons
         * and its reminder history row. No task ever has it, as task ids are UUIDs.
         */
        const val TASK_ID = "test-reminder"

        /** How far ahead the test fires: long enough to lock the phone or leave the app. */
        val LEAD: Duration = Duration.ofMinutes(1)
    }
}

/**
 * When the test reminder is due. It lives outside the database's tasks, so a test never adds one, and
 * it is kept on disk so the test still fires when the app is killed in the meantime.
 */
interface TestReminderStore {
    fun pendingAt(): Instant?

    /** Sets the instant, or clears it with null. */
    fun set(at: Instant?)
}

/**
 * Puts the test on the one scheduling path: [ReminderScheduler] arms the single alarm for it and posts it
 * when that alarm fires, the same as any task's reminder.
 */
class EngineTestReminder(
    private val store: TestReminderStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : TestReminder {
    override suspend fun send() = schedule(clock.instant().plus(TestReminder.LEAD))

    /** Sets the test to fire at [at], replacing one still waiting, and arms the alarm for it. */
    suspend fun schedule(at: Instant) {
        store.set(at)
        scheduler.run(FiredBy.CATCH_UP)
    }
}

/** [TestReminderStore] in private app preferences, written synchronously so a kill cannot lose it. */
class PrefsTestReminderStore(context: Context) : TestReminderStore {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun pendingAt(): Instant? = prefs.getLong(KEY_AT, NONE).takeIf { it != NONE }?.let(Instant::ofEpochMilli)

    override fun set(at: Instant?) {
        prefs.edit(commit = true) { if (at == null) remove(KEY_AT) else putLong(KEY_AT, at.toEpochMilli()) }
    }

    private companion object {
        const val FILE = "test_reminder"
        const val KEY_AT = "at_millis"
        const val NONE = Long.MIN_VALUE
    }
}
