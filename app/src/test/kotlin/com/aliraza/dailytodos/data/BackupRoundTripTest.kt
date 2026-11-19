package com.aliraza.dailytodos.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.backup.BackupReadResult
import com.aliraza.dailytodos.core.backup.Limits
import com.aliraza.dailytodos.core.backup.RejectReason
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.core.model.WeekStart
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.ReminderLogEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** AGENTS.md rule: export then import restores everything without loss. */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val clock = MutableClock(Instant.parse("2026-11-18T11:00:00Z"), ZoneId.of("Europe/Zurich"))
    private val databases = mutableListOf<AppDatabase>()

    private class Phone(val db: AppDatabase, clock: MutableClock) {
        val settings = SettingsRepository(db.settings()) {}
        val lists = ListRepository(db.lists(), clock)
        val tasks = TaskRepository(db, clock, settings) {}
        val subtasks = SubtaskRepository(db.subtasks())
        val backup = BackupRepository(db, settings, clock) {}
        val timers = TimerRepository(db.timer(), db.focusSessions(), settings, clock) {}
    }

    private fun newPhone() = Phone(
        AppDatabase.inMemory(ApplicationProvider.getApplicationContext()).also { databases += it },
        clock,
    )

    @After
    fun tearDown() = databases.forEach { it.close() }

    /** A database touching every table and every optional field. */
    private suspend fun Phone.fill() {
        settings.update {
            it.copy(
                headsUp = HeadsUp.TEN,
                theme = ThemeMode.DARK,
                overdueReminderTime = LocalTime.of(7, 45),
                clockFormat = ClockFormat.H24,
                weekStart = WeekStart.SUNDAY,
                haptics = false,
                keepScreenOn = true,
            )
        }
        lists.rename(AppDatabase.DEFAULT_LIST_ID, "Today stuff")
        val groceries = lists.create("Groceries")
        val bank = tasks.addTask(
            NewTask(
                title = "Call bank — ask about \"fees\" ✓",
                priority = Priority.P2,
                dueDate = LocalDate.of(2026, 11, 19),
                dueTime = LocalTime.of(16, 0),
                notes = "Account ends 1234\nAsk for a callback",
            ),
        )
        tasks.snooze(bank.id, Instant.parse("2026-11-19T15:10:00Z"))
        subtasks.add(bank.id, "Find statement")
        subtasks.add(bank.id, "Write questions").let { subtasks.update(it.copy(done = true)) }
        tasks.addTask(NewTask("Milk", listId = groceries.id))
        val done = tasks.addTask(NewTask("Old chore"))
        tasks.setCompleted(listOf(done.id), completed = true)
        val gone = tasks.addTask(NewTask("Deleted one"))
        timers.record(bank.id, Duration.ofMinutes(25), Instant.parse("2026-11-18T10:00:00Z"))
        timers.record(gone.id, Duration.ofMinutes(10), Instant.parse("2026-11-18T10:30:00Z"))
        tasks.setDeleted(listOf(gone.id), deleted = true)
        db.reminderLog().insert(
            ReminderLogEntity(
                taskId = bank.id,
                dueAt = Instant.parse("2026-11-18T09:00:00Z"),
                firedAt = Instant.parse("2026-11-18T09:00:01Z"),
                firedBy = FiredBy.ALARM,
            ),
        )
    }

    @Test
    fun exportThenImportOnAFreshPhoneRestoresEverything() = runTest {
        val original = newPhone().apply { fill() }
        val export = original.backup.export()
        original.backup.markBackedUp(export.at)

        val restored = newPhone()
        val read = restored.backup.read(export.text.byteInputStream())
        restored.backup.restore((read as BackupReadResult.Accepted).content)

        assertEquals(original.backup.snapshot(), restored.backup.snapshot())
        assertEquals(export.at, restored.settings.current().lastBackupAt)
        assertEquals(original.settings.current(), restored.settings.current())
        // Focus on the deleted task travels without it and still counts.
        assertEquals(
            listOf(Duration.ofMinutes(25), Duration.ofMinutes(10)),
            restored.backup.snapshot().focusSessions.map { it.focused },
        )
        assertEquals(null, restored.backup.snapshot().focusSessions.last().taskId)
    }

    @Test
    fun importReplacesWhatWasThere() = runTest {
        val source = newPhone().apply { fill() }
        val export = source.backup.export()

        val target = newPhone()
        target.tasks.addTask(NewTask("Will be replaced"))
        target.lists.create("Old list")
        val content = (target.backup.read(export.text.byteInputStream()) as BackupReadResult.Accepted).content
        target.backup.restore(content)

        val after = target.backup.snapshot()
        assertTrue(after.tasks.none { it.title == "Will be replaced" })
        assertTrue(after.lists.none { it.name == "Old list" })
        assertEquals(content.tasks.toSet(), after.tasks.toSet())
    }

    @Test
    fun deletedTasksAreNotExported() = runTest {
        val phone = newPhone().apply { fill() }
        clock.advance(Duration.ofMinutes(1))
        val snapshot = phone.backup.snapshot()
        assertTrue(snapshot.tasks.none { it.title == "Deleted one" })
    }

    @Test
    fun oversizedFileIsRefusedWithoutReadingItAll() {
        val phone = newPhone()
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int = if (served++ < Long.MAX_VALUE) '{'.code else -1
        }
        val result = phone.backup.read(endless)
        assertEquals(RejectReason.TOO_LARGE, (result as BackupReadResult.Rejected).reason)
        assertTrue(served <= Limits.MAX_FILE_BYTES + 64 * 1024 + 1)
    }

    @Test
    fun invalidUtf8IsRefused() {
        val bytes = byteArrayOf('{'.code.toByte(), 0xC3.toByte(), 0x28, '}'.code.toByte())
        val result = newPhone().backup.read(ByteArrayInputStream(bytes))
        assertEquals(RejectReason.MALFORMED, (result as BackupReadResult.Rejected).reason)
    }
}
