package com.aliraza.dailytodos.ui.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper
import java.time.Instant
import java.time.ZoneId

/** The Reminder health test reminder leaves the user's tasks alone. */
@RunWith(AndroidJUnit4::class)
class SendTestReminderTest {
    private val clock = MutableClock(Instant.parse("2026-11-18T11:00:00Z"), ZoneId.of("Europe/Zurich"))
    private val db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val settings = SettingsRepository(db.settings()) {}
    private val tasks = TaskRepository(db, clock, settings) {}
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
    }

    @Test
    fun sendingATestReminderAddsNoTask() {
        var sent = 0
        val vm = viewModels.add {
            SettingsViewModel(
                settings,
                BackupRepository(db, settings, clock) {},
                tasks,
                ListRepository(db.lists(), clock),
                db.reminderLog(),
                clock,
                testReminder = { sent++ },
            )
        }
        vm.sendTestReminder()
        settle { sent > 0 || runBlocking { db.tasks().allKept() }.isNotEmpty() }

        assertEquals(1, sent)
        assertTrue(runBlocking { db.tasks().allKept() }.isEmpty())
    }

    /** Runs the ViewModel's work until [done] or a time limit passes. */
    private fun settle(done: () -> Boolean) {
        val deadline = System.nanoTime() + SETTLE_NANOS
        while (!done() && System.nanoTime() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(POLL_MS)
        }
    }

    private companion object {
        const val SETTLE_NANOS = 2_000_000_000L
        const val POLL_MS = 10L
    }
}
