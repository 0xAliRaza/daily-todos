package com.aliraza.dailytodos.ui.tasks

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.SubtaskRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The undo snackbar names the one task an action touched ("“Call bank” completed") and counts several
 * ("2 tasks completed"); moving tasks to another list undoes back to each one's own list and place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TasksViewModelUndoTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 21)
    private val clock = MutableClock(LocalDateTime.of(today, LocalTime.NOON).atZone(zone).toInstant(), zone)
    private val resources: Resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var lists: ListRepository
    private lateinit var viewModel: TasksViewModel
    private val viewModels = TestViewModels()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val settings = SettingsRepository(db.settings()) {}
        tasks = TaskRepository(db, clock, settings) {}
        lists = ListRepository(db.lists(), clock)
        viewModel = viewModels.add { TasksViewModel(tasks, SubtaskRepository(db.subtasks()), lists, settings, clock) }
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun completingOneTaskNamesIt() = runBlocking {
        val call = tasks.addTask(NewTask("Call bank", dueDate = today))
        val event = nextUndo { viewModel.setDone(listOf(call.id), done = true) }
        assertEquals(UndoAction.COMPLETED, event.action)
        assertEquals("Call bank", event.title)
        assertEquals("“Call bank” completed", event.text(resources))
    }

    @Test
    fun completingSeveralCountsThem() = runBlocking {
        val call = tasks.addTask(NewTask("Call bank", dueDate = today))
        val rent = tasks.addTask(NewTask("Pay rent", dueDate = today))
        val event = nextUndo { viewModel.setDone(listOf(call.id, rent.id), done = true) }
        assertNull(event.title)
        assertEquals("2 tasks completed", event.text(resources))
    }

    @Test
    fun deletingAndReschedulingOneTaskNameIt() = runBlocking {
        val call = tasks.addTask(NewTask("Call bank", dueDate = today))
        val moved = nextUndo { viewModel.reschedule(listOf(call.id), today.plusDays(1)) }
        assertEquals("“Call bank” rescheduled", moved.text(resources))
        val deleted = nextUndo { viewModel.delete(listOf(call.id)) }
        assertEquals("“Call bank” deleted", deleted.text(resources))
    }

    @Test
    fun movingOneTaskNamesItAndTheList() = runBlocking {
        val work = workList()
        val call = tasks.addTask(NewTask("Call bank"))
        val event = nextUndo { viewModel.moveToList(listOf(call.id), work.id) }
        assertEquals(work.id, tasks.task(call.id)?.listId)
        assertEquals("“Call bank” moved to Work", event.text(resources))
    }

    @Test
    fun movingSeveralUndoesToEachOwnListAndPlace() = runBlocking {
        val work = workList()
        val first = tasks.addTask(NewTask("Water the plants"))
        val call = tasks.addTask(NewTask("Call bank"))
        val last = tasks.addTask(NewTask("Read 20 pages"))
        val report = tasks.addTask(NewTask("Submit report", listId = work.id))
        val home = first.listId

        val event = nextUndo { viewModel.moveToList(listOf(call.id, report.id, last.id), work.id) }
        // The report was in Work already, so only two moved.
        assertEquals(2, event.count)
        assertEquals("2 tasks moved to Work", event.text(resources))
        assertEquals(work.id, tasks.task(call.id)?.listId)

        event.undo()
        withTimeout(TIMEOUT_MS) { while (tasks.task(last.id)?.listId != home) delay(POLL_MS) }
        withTimeout(TIMEOUT_MS) { while (tasks.task(last.id)?.position != last.position) delay(POLL_MS) }
        assertEquals(home to call.position, tasks.task(call.id)?.let { it.listId to it.position })
        assertEquals(home to last.position, tasks.task(last.id)?.let { it.listId to it.position })
        assertEquals(work.id to report.position, tasks.task(report.id)?.let { it.listId to it.position })
    }

    private suspend fun workList(): TaskList {
        val work = lists.create("Work")
        // The move names the list from the view model's own copy, so wait until it has it.
        withTimeout(TIMEOUT_MS) { viewModel.lists.first { all -> all.any { it.id == work.id } } }
        return work
    }

    /** Runs [action] and returns the undo it offers. */
    private suspend fun nextUndo(action: () -> Unit): UndoEvent = coroutineScope {
        val event: Deferred<UndoEvent> = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TIMEOUT_MS) { viewModel.undo.first() }
        }
        action()
        event.await()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
    }
}
