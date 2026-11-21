package com.aliraza.dailytodos.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.data.ListRepository
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
    private val testStore = FakeTestReminderStore()

    private lateinit var db: AppDatabase
    private lateinit var mirror: ScheduleMirror
    private lateinit var settings: SettingsRepository
    private lateinit var tasks: TaskRepository
    private lateinit var lists: ListRepository
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
        lists = ListRepository(db.lists(), clock) {}
        scheduler = ReminderScheduler(
            tasks,
            lists,
            settings,
            db.reminderLog(),
            poster,
            alarm,
            mirror,
            testStore,
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
        assertEquals(ReminderKind.OVERDUE, poster.posted.single().kind)
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
            lists,
            settings,
            db.reminderLog(),
            poster,
            alarm,
            ScheduleMirror(blocked),
            testStore,
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
        val actions = ReminderActions(tasks, poster, EngineTestReminder(testStore, scheduler, clock), clock)
        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)

        actions.handle(gym.id, AndroidReminderPoster.Action.SNOOZE)
        assertFalse(gym.id in poster.shownTaskIds())
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(at(16).plus(Duration.ofMinutes(10)), alarm.armedAt)

        actions.handle(gym.id, AndroidReminderPoster.Action.COMPLETE)
        assertTrue(tasks.openDated().isEmpty())
    }

    @Test
    fun aReminderNamesItsKindDueMomentAndList() = runTest {
        settings.update { it.copy(headsUp = HeadsUp.TEN) }
        val work = lists.create("Work")
        tasks.addTask(NewTask("Standup", listId = work.id, dueDate = today, dueTime = LocalTime.of(11, 30)))
        add("Call bank", 16)

        clock.now = at(11, 20)
        scheduler.run(FiredBy.ALARM)
        val standup = poster.posted.single()
        assertEquals(ReminderKind.HEADS_UP, standup.kind)
        assertEquals(at(11, 20), standup.at)
        assertEquals(today to LocalTime.of(11, 30), standup.dueDate to standup.dueTime)
        assertEquals("Work", standup.listName)

        clock.now = at(16)
        scheduler.run(FiredBy.ALARM)
        val bank = poster.posted.last()
        assertEquals(ReminderKind.DUE, bank.kind)
        assertEquals(LocalTime.of(16, 0), bank.dueTime)
        assertEquals("Daily", bank.listName)
    }

    @Test
    fun theMirrorWordsRemindersLikeTheEngine() = runTest {
        settings.update { it.copy(headsUp = HeadsUp.TEN, clockFormat = ClockFormat.H24) }
        add("Gym", 16)
        scheduler.run(FiredBy.CATCH_UP)
        val entry = mirror.read().upcoming.single()
        assertEquals("Daily", entry.listName)
        assertEquals(at(16).toEpochMilli(), entry.dueMillis)
        assertTrue(entry.timed)
        assertEquals(at(15, 50).toEpochMilli(), entry.atMillis)

        // Restart; still locked at the heads-up.
        clock.now = at(15, 50)
        val lockedPoster = FakePoster()
        BeforeUnlockReminders(mirror, lockedPoster, FakeAlarm(), clock).run()
        val content = lockedPoster.posted.single()
        assertEquals(ReminderKind.HEADS_UP, content.kind)
        assertEquals(today to LocalTime.of(16, 0), content.dueDate to content.dueTime)
        assertEquals("Daily", content.listName)
        assertEquals(ClockFormat.H24, content.clockFormat)
    }

    @Test
    fun aMirrorFromBeforeListNamesStillPosts() = runTest {
        val file = File(folder.root, "old-mirror.json")
        file.writeText("""{"version":1,"upcoming":[{"taskId":"a","title":"Gym","atMillis":${at(8).toEpochMilli()}}]}""")
        val lockedPoster = FakePoster()
        BeforeUnlockReminders(ScheduleMirror(file), lockedPoster, FakeAlarm(), clock).run()
        val content = lockedPoster.posted.single()
        assertEquals("Gym", content.title)
        assertNull(content.listName)
        assertEquals(ReminderKind.DUE, content.kind)
    }

    @Test
    fun theTestReminderTakesTheOneAlarmAndTouchesNoTask() = runTest {
        add("Gym", 16)
        val test = EngineTestReminder(testStore, scheduler, clock)
        test.send()
        assertEquals(at(8, 1), alarm.armedAt)

        clock.now = at(8, 1)
        scheduler.run(FiredBy.ALARM)
        val posted = poster.posted.single()
        assertEquals(TestReminder.TASK_ID, posted.taskId)
        assertEquals(ReminderKind.DUE, posted.kind)
        assertEquals(LocalTime.of(8, 1), posted.dueTime)
        assertEquals("Daily", posted.listName)
        assertNull(testStore.at)
        assertEquals(at(16), alarm.armedAt)

        val log = db.reminderLog().observeAll().first().single()
        assertEquals(TestReminder.TASK_ID, log.taskId)
        assertEquals(at(8, 1), log.firedAt)
        assertEquals(listOf("Gym"), tasks.openDated().map { it.title })
        assertNull(tasks.openDated().single().notifiedFor)

        // Later runs leave the test in the shade; it belongs to no task that could make it stale.
        scheduler.run(FiredBy.CATCH_UP)
        assertTrue(TestReminder.TASK_ID in poster.shownTaskIds())
        assertEquals(1, poster.posted.size)
    }

    @Test
    fun theTestReminderButtonsTouchNoTask() = runTest {
        val gym = add("Gym", 16)
        val test = EngineTestReminder(testStore, scheduler, clock)
        val actions = ReminderActions(tasks, poster, test, clock)
        test.send()
        clock.now = at(8, 1)
        scheduler.run(FiredBy.ALARM)

        actions.handle(TestReminder.TASK_ID, AndroidReminderPoster.Action.SNOOZE)
        assertFalse(TestReminder.TASK_ID in poster.shownTaskIds())
        assertEquals(at(8, 11), alarm.armedAt)

        clock.now = at(8, 11)
        scheduler.run(FiredBy.ALARM)
        actions.handle(TestReminder.TASK_ID, AndroidReminderPoster.Action.COMPLETE)
        assertTrue(poster.shownTaskIds().isEmpty())
        assertEquals(listOf(gym.id), tasks.openDated().map { it.id })
        assertNull(tasks.task(gym.id)!!.snoozedUntil)
    }
}
