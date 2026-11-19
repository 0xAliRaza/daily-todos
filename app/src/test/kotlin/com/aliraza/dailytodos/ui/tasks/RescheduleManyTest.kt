package com.aliraza.dailytodos.ui.tasks

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.SubtaskRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

/** "Reschedule" on the overdue header moves every overdue task at once, and one undo puts each back. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RescheduleManyTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = MutableClock(LocalDateTime.of(today, LocalTime.NOON).atZone(zone).toInstant(), zone)

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var viewModel: TasksViewModel
    private val viewModels = TestViewModels()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val settings = SettingsRepository(db.settings()) {}
        tasks = TaskRepository(db, clock, settings) {}
        viewModel = viewModels.add {
            TasksViewModel(
                tasks,
                SubtaskRepository(db.subtasks()),
                ListRepository(db.lists(), clock),
                settings,
                clock,
            )
        }
    }

    /** The ViewModel's reads stop before the database closes under them. */
    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun allMoveKeepingTheirTimesAndOneUndoRestoresEach() = runBlocking {
        val nine = LocalTime.of(9, 0)
        val call = tasks.addTask(NewTask("Call bank", dueDate = today.minusDays(2), dueTime = nine))
        val rent = tasks.addTask(NewTask("Pay rent", dueDate = today.minusDays(1)))
        val event = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(TIMEOUT_MS) { viewModel.undo.first() } }

        viewModel.reschedule(listOf(call.id, rent.id), today)
        val undo = event.await()

        assertEquals(2, undo.count)
        assertEquals(today to nine, tasks.task(call.id)?.let { it.dueDate to it.dueTime })
        assertEquals(today, tasks.task(rent.id)?.dueDate)

        undo.undo()
        withTimeout(TIMEOUT_MS) { while (tasks.task(rent.id)?.dueDate == today) delay(POLL_MS) }
        assertEquals(today.minusDays(2) to nine, tasks.task(call.id)?.let { it.dueDate to it.dueTime })
        assertEquals(today.minusDays(1), tasks.task(rent.id)?.dueDate)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
    }
}
