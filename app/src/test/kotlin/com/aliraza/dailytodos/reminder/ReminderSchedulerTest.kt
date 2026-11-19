package com.aliraza.dailytodos.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = MutableClock(at(8, 0), zone)
    private val poster = FakePoster()
    private val alarm = FakeAlarm()

    private lateinit var db: AppDatabase
    private lateinit var mirror: ScheduleMirror
    private lateinit var settings: SettingsRepository
    private lateinit var tasks: TaskRepository
    private lateinit var scheduler: ReminderScheduler
    private var timerEnd: Instant? = null

    private fun at(hour: Int, minute: Int = 0, date: LocalDate = today): Instant =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant()

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        mirror = ScheduleMirror(File(folder.root, ScheduleMirror.FILE_NAME))
        // Writes do not re-run the engine here, so each test controls exactly when it runs.
        settings = SettingsRepository(db.settings()) {}
        tasks = TaskRepository(db, clock, settings) {}
        scheduler = ReminderScheduler(
            tasks,
            settings,
            db.reminderLog(),
            poster,
            alarm,
            mirror,
            clock,
        ) { _, _ -> timerEnd }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(title: String, hour: Int, minute: Int = 0) =
        tasks.addTask(NewTask(title, dueDate = today, dueTime = LocalTime.of(hour, minute)))

    @Test
    fun armsTheAlarmForTheEarliestReminder() = runTest {
        add("Later", 18)
        add("Sooner", 16)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(at(16), alarm.armedAt)
        assertTrue(poster.posted.isEmpty())
    }

    @Test
    fun alarmPostsWhatIsDueMarksItAndArmsTheNext() = runTest {
        val gym = add("Gym", 16)
        add("Dinner", 19)
        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)

        assertEquals(listOf("Gym"), poster.posted.map { it.title })
        assertEquals(at(16), tasks.task(gym.id)!!.notifiedFor)
        assertEquals(at(19), alarm.armedAt)
        val log = db.reminderLog().observeAll().first().single()
        assertEquals(FiredBy.ALARM to at(16), log.firedBy to log.dueAt)
    }

    @Test
    fun aSecondRunDoesNotPostTheSameReminderAgain() = runTest {
        add("Gym", 16)
        clock.now = at(16, 5)
        scheduler.run(FiredBy.ALARM)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(1, poster.posted.size)
    }

    @Test
    fun nothingPendingCancelsTheAlarm() = runTest {
        scheduler.run(FiredBy.CATCH_UP)
        assertTrue(alarm.cancelled)
        assertNull(alarm.armedAt)
    }

    @Test
    fun overdueReminderSaysSo() = runTest {
        add("Gym", 16)
        clock.now = at(9, 0, today.plusDays(1))
        scheduler.run(FiredBy.ALARM)
        assertTrue(poster.posted.single().overdue)
    }

    @Test
    fun reminderForADoneTaskIsRemoved() = runTest {
        val gym = add("Gym", 16)
        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)
        assertEquals(setOf(gym.id), poster.shownTaskIds())

        tasks.setCompleted(listOf(gym.id), completed = true)
        scheduler.run(FiredBy.CATCH_UP)
        assertTrue(poster.shownTaskIds().isEmpty())
    }

    @Test
    fun reminderForATaskMovedToLaterIsRemoved() = runTest {
        val gym = add("Gym", 16)
        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)
        tasks.updateTask(gym.id) { it.copy(dueTime = LocalTime.of(20, 0)) }
        scheduler.run(FiredBy.CATCH_UP)
        assertTrue(poster.shownTaskIds().isEmpty())
        assertEquals(at(20), alarm.armedAt)
    }

    @Test
    fun whenNotificationsAreBlockedTheReminderIsRetriedLater() = runTest {
        val gym = add("Gym", 16)
        clock.now = at(16)
        poster.allowed = false
        scheduler.run(FiredBy.ALARM)
        assertNull(tasks.task(gym.id)!!.notifiedFor)
        assertTrue(db.reminderLog().observeAll().first().isEmpty())

        poster.allowed = true
        clock.now = at(16, 30)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(listOf("Gym"), poster.posted.map { it.title })
    }

    @Test
    fun aRunningTimerSharesTheSingleAlarm() = runTest {
        add("Gym", 16)
        timerEnd = at(8, 25)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(at(8, 25), alarm.armedAt)
    }

    @Test
    fun mirrorHoldsTheUpcomingRemindersAndTheDndSetting() = runTest {
        add("Gym", 16)
        add("Dinner", 19)
        settings.update { it.copy(dndOverride = true) }
        scheduler.run(FiredBy.CATCH_UP)
        val snapshot = mirror.read()
        assertEquals(listOf("Gym", "Dinner"), snapshot.upcoming.map { it.title })
        assertEquals(at(16).toEpochMilli(), snapshot.upcoming.first().atMillis)
        assertTrue(snapshot.dndOverride)
    }

    @Test
    fun aMirrorThatCannotBeWrittenDoesNotStopTheEngine() = runTest {
        add("Gym", 16)
        // The parent "folder" is a plain file, so every write fails.
        val blocked = File(folder.newFile("not-a-folder"), ScheduleMirror.FILE_NAME)
        val failing = ReminderScheduler(
            tasks,
            settings,
            db.reminderLog(),
            poster,
            alarm,
            ScheduleMirror(blocked),
            clock,
        )
        failing.run(FiredBy.CATCH_UP)
        assertEquals(at(16), alarm.armedAt)
    }

    @Test
    fun historyKeepsTheNewestEntriesOnly() = runTest {
        repeat(ReminderScheduler.LOG_SIZE + 5) { add("Task $it", 9) }
        clock.now = at(9)
        scheduler.run(FiredBy.ALARM)
        assertEquals(ReminderScheduler.LOG_SIZE, db.reminderLog().observeAll().first().size)
    }

    @Test
    fun beforeUnlockPostsFromTheMirrorOnceAndRecordsItAfterUnlock() = runTest {
        val gym = add("Gym", 16)
        add("Dinner", 19)
        scheduler.run(FiredBy.CATCH_UP)

        // Restart; the phone stays locked past 16:00.
        clock.now = at(16, 2)
        val lockedPoster = FakePoster()
        val lockedAlarm = FakeAlarm()
        val beforeUnlock = BeforeUnlockReminders(mirror, lockedPoster, lockedAlarm, clock)
        beforeUnlock.run()
        beforeUnlock.run()
        assertEquals(listOf("Gym"), lockedPoster.posted.map { it.title })
        assertEquals(at(19), lockedAlarm.armedAt)

        // First unlock: the engine records it instead of posting it again.
        clock.now = at(16, 10)
        poster.posted.clear()
        scheduler.run(FiredBy.CATCH_UP)
        assertTrue(poster.posted.isEmpty())
        assertEquals(at(16), tasks.task(gym.id)!!.notifiedFor)
        val log = db.reminderLog().observeAll().first().single()
        assertEquals(FiredBy.BEFORE_UNLOCK, log.firedBy)
        assertEquals(at(16, 2), log.firedAt)
        assertTrue(mirror.read().firedBeforeUnlock.isEmpty())
    }

    @Test
    fun snoozeAndDoneActions() = runTest {
        val gym = add("Gym", 16)
        val actions = ReminderActions(tasks, poster, clock)
        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)

        actions.handle(gym.id, AndroidReminderPoster.Action.SNOOZE_SHORT)
        assertFalse(gym.id in poster.shownTaskIds())
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(at(16).plus(Duration.ofMinutes(10)), alarm.armedAt)

        actions.handle(gym.id, AndroidReminderPoster.Action.DONE)
        assertTrue(tasks.openDated().isEmpty())
    }
}
