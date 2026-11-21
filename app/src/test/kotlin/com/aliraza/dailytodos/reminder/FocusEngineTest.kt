package com.aliraza.dailytodos.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusRounds
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.TimerRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
import java.time.ZoneId

/** The focus timer travels the scheduler's single alarm, like every reminder, and records its focus. */
@RunWith(AndroidJUnit4::class)
class FocusEngineTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val start = Instant.parse("2026-11-18T09:00:00Z")
    private val clock = MutableClock(start, ZoneId.of("Europe/Zurich"))
    private val alarm = FakeAlarm()
    private val signals = FakeSignals()

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var timers: TimerRepository
    private lateinit var scheduler: ReminderScheduler
    private lateinit var taskId: String

    @Before
    fun setUp() = runTest {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val settings = SettingsRepository(db.settings()) {}
        // Every write re-runs the scheduler, as in the app.
        tasks = TaskRepository(db, clock, settings) { scheduler.run(FiredBy.CATCH_UP) }
        timers = TimerRepository(db.timer(), db.focusSessions(), settings, clock) { scheduler.run(FiredBy.CATCH_UP) }
        val mirror = ScheduleMirror(File(folder.root, ScheduleMirror.FILE_NAME))
        val lists = ListRepository(db.lists(), clock)
        scheduler = ReminderScheduler(
            tasks,
            lists,
            settings,
            db.reminderLog(),
            FakePoster(),
            alarm,
            mirror,
            FakeTestReminderStore(),
            clock,
            timer = FocusEngine(timers, tasks, signals),
        )
        // An undated task, so no reminder shares the alarm with the timer.
        val work = lists.create("Work")
        taskId = tasks.addTask(NewTask("Write report", listId = work.id)).id
    }

    @After
    fun tearDown() = db.close()

    private suspend fun timersList(): String = tasks.task(taskId)!!.listId

    private fun minutes(n: Long): Instant = start.plus(Duration.ofMinutes(n))

    private suspend fun sessions() = timers.observeSessions().first()

    private suspend fun focusEnded() {
        timers.start(taskId)
        clock.now = minutes(25)
        scheduler.run(FiredBy.ALARM)
    }

    @Test
    fun aRunningRoundArmsTheSingleAlarm() = runTest {
        timers.start(taskId)
        assertEquals(minutes(25), alarm.armedAt)
        assertEquals(FocusPhase.FOCUS, signals.progress?.phase)
        assertEquals("Write report", signals.progressTitle)
    }

    @Test
    fun focusEndAlertsWithTheTaskRecordsTheRoundAndTheBreakWaits() = runTest {
        focusEnded()
        assertEquals(listOf(Alert(FocusPhase.FOCUS, taskId, "Write report")), signals.alerts)
        assertEquals(FocusPhase.BREAK_DUE, timers.current().phase)
        assertNull(alarm.armedAt)
        val recorded = sessions().single()
        assertEquals(taskId, recorded.taskId)
        assertEquals(Duration.ofMinutes(25), recorded.focused)
        assertEquals(minutes(25), recorded.endedAt)
    }

    @Test
    fun aLateAlarmStillRecordsTheRoundAtItsRealEnd() = runTest {
        timers.start(taskId)
        clock.now = minutes(40)
        scheduler.run(FiredBy.ALARM)
        assertEquals(minutes(25), sessions().single().endedAt)
    }

    @Test
    fun startingTheBreakArmsItsEndAndDropsTheAlert() = runTest {
        focusEnded()
        clock.now = minutes(27)
        timers.startBreak()
        assertEquals(FocusPhase.BREAK, timers.current().phase)
        assertEquals(minutes(32), alarm.armedAt)
        assertEquals(FocusPhase.BREAK, signals.synced?.phase)
    }

    @Test
    fun skippingTheBreakLeavesTheTimerIdleAndSyncsTheAlertAway() = runTest {
        focusEnded()
        timers.skipBreak()
        assertEquals(FocusTimer.Idle, timers.current())
        assertEquals(FocusTimer.Idle, signals.synced)
        assertEquals(false, FocusRounds.alertApplies(FocusPhase.FOCUS, taskId, signals.synced!!))
    }

    @Test
    fun breakEndAlertsOffersTheNextFocusAndLeavesNoAlarm() = runTest {
        focusEnded()
        timers.startBreak()
        clock.now = minutes(30)
        scheduler.run(FiredBy.ALARM)
        assertEquals(Alert(FocusPhase.BREAK, taskId, "Write report"), signals.alerts.last())
        assertEquals(FocusTimer.Idle, timers.current())
        assertNull(alarm.armedAt)
        assertNull(signals.progress?.endsAt)
        // Only focus counts.
        assertEquals(1, sessions().size)
    }

    @Test
    fun stoppingFocusEarlyKeepsTheTimeSpent() = runTest {
        timers.start(taskId)
        clock.now = minutes(10)
        timers.stop()
        assertEquals(FocusTimer.Idle, timers.current())
        assertEquals(Duration.ofMinutes(10), sessions().single().focused)
        assertTrue(signals.alerts.isEmpty())
    }

    @Test
    fun lessThanAMinuteIsNotRecorded() = runTest {
        timers.start(taskId)
        clock.now = start.plusSeconds(40)
        timers.stop()
        assertTrue(sessions().isEmpty())
    }

    @Test
    fun completingTheTaskStopsItsTimerQuietlyAndKeepsTheTimeSpent() = runTest {
        timers.start(taskId)
        clock.now = minutes(12)
        tasks.setCompleted(listOf(taskId), completed = true)
        assertEquals(FocusTimer.Idle, timers.current())
        assertNull(alarm.armedAt)
        assertTrue(signals.alerts.isEmpty())
        assertEquals(Duration.ofMinutes(12), sessions().single().focused)
    }

    @Test
    fun deletingTheTaskWhileTheBreakWaitsStopsTheTimer() = runTest {
        focusEnded()
        tasks.setDeleted(listOf(taskId), deleted = true)
        assertEquals(FocusTimer.Idle, timers.current())
        assertEquals(FocusTimer.Idle, signals.synced)
    }

    @Test
    fun startingAnotherTaskEndsTheFirstAndKeepsItsTime() = runTest {
        val other = tasks.addTask(NewTask("Plan trip", listId = timersList())).id
        timers.start(taskId)
        clock.now = minutes(5)
        timers.start(other)
        assertEquals(other, timers.current().taskId)
        assertEquals(minutes(30), alarm.armedAt)
        assertEquals(listOf(taskId to Duration.ofMinutes(5)), sessions().map { it.taskId to it.focused })
    }

    @Test
    fun aTaskDeletedForGoodKeepsItsTimeInTheTotals() = runTest {
        focusEnded()
        db.tasks().deleteAll()
        assertNull(sessions().single().taskId)
    }

    private data class Alert(val ended: FocusPhase, val taskId: String?, val title: String)

    private class FakeSignals : FocusSignals {
        val alerts = mutableListOf<Alert>()
        var synced: FocusTimer? = null
        var progress: FocusTimer? = null
        var progressTitle: String? = null

        override fun alert(ended: FocusTimer, title: String, settings: Settings) {
            alerts += Alert(ended.phase, ended.taskId, title)
        }

        override fun syncAlert(timer: FocusTimer) {
            synced = timer
        }

        override fun showProgress(timer: FocusTimer, title: String, settings: Settings) {
            progress = timer
            progressTitle = title
        }
    }
}
