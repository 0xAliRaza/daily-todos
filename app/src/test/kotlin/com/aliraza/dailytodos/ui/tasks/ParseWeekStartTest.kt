package com.aliraza.dailytodos.ui.tasks

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.core.model.WeekStart
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.SubtaskRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** "next week" in the add bar starts the week on the day the "Week starts on" setting names. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ParseWeekStartTest {
    private val zone = ZoneId.of("Europe/Zurich")

    /** A Wednesday. */
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = MutableClock(LocalDateTime.of(today, LocalTime.NOON).atZone(zone).toInstant(), zone)

    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var viewModel: TasksViewModel
    private val viewModels = TestViewModels()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        settings = SettingsRepository(db.settings()) {}
        val tasks = TaskRepository(db, clock, settings) {}
        viewModel = viewModels.add {
            TasksViewModel(tasks, SubtaskRepository(db.subtasks()), ListRepository(db.lists(), clock), settings, clock)
        }
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
        Dispatchers.resetMain()
    }

    private fun nextWeekWith(start: WeekStart): LocalDate? = runBlocking {
        settings.update { it.copy(weekStart = start) }
        withTimeout(TIMEOUT_MS) { viewModel.settings.first { it.weekStart == start } }
        viewModel.parse("Plan the trip next week", excluded = emptySet()).date
    }

    @Test
    fun nextWeekFollowsTheWeekStartSetting() {
        assertEquals(LocalDate.of(2026, 11, 23), nextWeekWith(WeekStart.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 22), nextWeekWith(WeekStart.SUNDAY))
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
