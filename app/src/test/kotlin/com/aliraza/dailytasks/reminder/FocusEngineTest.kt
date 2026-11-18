package com.aliraza.dailytasks.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytasks.MutableClock
import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.FiredBy
import com.aliraza.dailytasks.core.model.NewTask
import com.aliraza.dailytasks.core.model.TimerSignal
import com.aliraza.dailytasks.data.SettingsRepository
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.TimerRepository
import com.aliraza.dailytasks.data.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** The focus timer travels the scheduler's single alarm, like every reminder. */
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

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val settings = SettingsRepository(db.settings()) {}
        tasks = TaskRepository(db, clock, settings) {}
        timers = TimerRepository(db.timer(), settings, clock) {}
        val mirror = ScheduleMirror(File(folder.root, ScheduleMirror.FILE_NAME))
        scheduler = ReminderScheduler(
            tasks,
            settings,
            db.reminderLog(),
            FakePoster(),
            alarm,
            mirror,
            clock,
            timer = FocusEngine(timers, tasks, signals),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun minutes(n: Long): Instant = start.plus(Duration.ofMinutes(n))

    @Test
    fun aRunningRoundArmsTheSingleAlarm() = runTest {
        timers.start(taskId = null)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(minutes(25), alarm.armedAt)
        assertEquals(FocusPhase.FOCUS, signals.progress?.phase)
    }

    @Test
    fun theAlarmEndsFocusAlertsAndArmsTheBreakEnd() = runTest {
        timers.start(taskId = null)
        clock.now = minutes(25)
        scheduler.run(FiredBy.ALARM)
        assertEquals(listOf(FocusPhase.FOCUS to TimerSignal.BOTH), signals.alerts)
        assertEquals(FocusPhase.BREAK, timers.current().phase)
        assertEquals(minutes(30), alarm.armedAt)
    }

    @Test
    fun breakEndWaitsAndLeavesNoAlarm() = runTest {
        timers.start(taskId = null)
        clock.now = minutes(30)
        scheduler.run(FiredBy.ALARM)
        assertEquals(FocusPhase.WAITING, timers.current().phase)
        assertNull(alarm.armedAt)
        assertNull(signals.progress?.endsAt)
    }

    @Test
    fun completingTheTaskStopsTheTimerWithoutAnAlert() = runTest {
        val task = tasks.addTask(NewTask(title = "Write the report"))
        timers.start(task.id)
        tasks.setCompleted(listOf(task.id), completed = true)
        scheduler.run(FiredBy.CATCH_UP)
        assertEquals(FocusTimer.Idle, timers.current())
        assertEquals(emptyList<Pair<FocusPhase, TimerSignal>>(), signals.alerts)
        assertEquals(1, signals.cleared)
    }

    private class FakeSignals : FocusSignals {
        val alerts = mutableListOf<Pair<FocusPhase, TimerSignal>>()
        var cleared = 0
        var progress: FocusTimer? = null

        override fun alert(ended: FocusPhase, signal: TimerSignal) {
            alerts += ended to signal
        }

        override fun clearAlert() {
            cleared++
        }

        override fun showProgress(timer: FocusTimer) {
            progress = timer
        }
    }
}
