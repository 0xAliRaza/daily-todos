package com.aliraza.dailytasks.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytasks.MutableClock
import com.aliraza.dailytasks.core.model.HeadsUp
import com.aliraza.dailytasks.core.model.NewTask
import com.aliraza.dailytasks.core.model.Priority
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val noon: Instant = LocalDateTime.of(today, LocalTime.NOON).atZone(zone).toInstant()
    private val clock = MutableClock(noon, zone)

    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var lists: ListRepository
    private lateinit var settings: SettingsRepository
    private var scheduleRuns = 0

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val onScheduleChanged: suspend () -> Unit = { scheduleRuns++ }
        settings = SettingsRepository(db.settings(), onScheduleChanged)
        lists = ListRepository(db.lists())
        repository = TaskRepository(db, clock, settings, onScheduleChanged)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun freshDatabaseHasOnlyTheDefaultDailyList() = runTest {
        val lists = lists.observe().first()
        assertEquals(1, lists.size)
        assertEquals("Daily", lists.single().name)
        assertTrue(lists.single().isDefault)
    }

    @Test
    fun undatedTaskInDailyGetsTomorrowButNotInOtherLists() = runTest {
        val daily = repository.addTask(NewTask("Call bank"))
        assertEquals(today.plusDays(1), daily.dueDate)
        assertNull(daily.dueTime)

        val groceries = lists.create("Groceries")
        val milk = repository.addTask(NewTask("Milk", listId = groceries.id))
        assertNull(milk.dueDate)
        assertEquals(groceries.id, milk.listId)
    }

    @Test
    fun newTasksGoToTheBottomOfTheirList() = runTest {
        val first = repository.addTask(NewTask("One"))
        val second = repository.addTask(NewTask("Two"))
        assertEquals(first.position + 1, second.position)
    }

    @Test
    fun unknownListFallsBackToTheDefaultList() = runTest {
        val task = repository.addTask(NewTask("Orphan", listId = "missing"))
        assertEquals(AppDatabase.DEFAULT_LIST_ID, task.listId)
    }

    @Test
    fun taskTypedForAnEarlierTimeTodayDoesNotPingAtOnce() = runTest {
        val task = repository.addTask(NewTask("Gym", dueDate = today, dueTime = LocalTime.of(8, 0)))
        assertNotNull(task.notifiedFor)
        assertEquals(task, repository.task(task.id))
    }

    @Test
    fun changingTheTimeClearsReminderMarksButOtherEditsKeepThem() = runTest {
        val task = repository.addTask(NewTask("Gym", dueDate = today, dueTime = LocalTime.of(8, 0)))
        repository.updateTask(task.id) { it.copy(priority = Priority.P1, snoozedUntil = noon) }
        val renamed = repository.task(task.id)!!
        assertEquals(task.notifiedFor, renamed.notifiedFor)
        assertEquals(noon, renamed.snoozedUntil)

        repository.updateTask(task.id) { it.copy(dueTime = LocalTime.of(18, 0)) }
        val moved = repository.task(task.id)!!
        assertNull(moved.notifiedFor)
        assertNull(moved.snoozedUntil)
    }

    @Test
    fun movingToAnotherListPutsTheTaskAtTheBottom() = runTest {
        val groceries = lists.create("Groceries")
        repository.addTask(NewTask("Milk", listId = groceries.id))
        val bread = repository.addTask(NewTask("Bread"))
        repository.updateTask(bread.id) { it.copy(listId = groceries.id) }
        assertEquals(1L, repository.task(bread.id)!!.position)
    }

    @Test
    fun completeAndDeleteHideTasksAndUndoBringsThemBack() = runTest {
        val task = repository.addTask(NewTask("Call bank"))
        repository.setCompleted(listOf(task.id), true)
        assertTrue(repository.observeOpenDated().first().isEmpty())
        repository.setCompleted(listOf(task.id), false)
        assertEquals(listOf(task.id), repository.observeOpenDated().first().map { it.id })

        repository.setDeleted(listOf(task.id), true)
        assertTrue(repository.openDated().isEmpty())
        repository.setDeleted(listOf(task.id), false)
        assertEquals(listOf(task.id), repository.openDated().map { it.id })
    }

    @Test
    fun purgeRemovesOnlyWhatIsOlderThanTheUndoWindow() = runTest {
        val old = repository.addTask(NewTask("Old"))
        repository.addSubtask(old.id, "Step")
        repository.setDeleted(listOf(old.id), true)

        clock.advance(Duration.ofDays(2))
        val recent = repository.addTask(NewTask("Recent"))
        repository.setDeleted(listOf(recent.id), true)

        repository.purgeDeleted()
        assertNull(repository.task(old.id))
        assertTrue(repository.observeSubtasks(old.id).first().isEmpty())
        assertNotNull(repository.task(recent.id))
    }

    @Test
    fun searchMatchesTitleAndNotesAndTreatsWildcardsLiterally() = runTest {
        repository.addTask(NewTask("Pay 50% deposit"))
        repository.addTask(NewTask("Call", notes = "ask about the deposit"))
        repository.addTask(NewTask("Unrelated"))
        assertEquals(2, repository.search("deposit").first().size)
        assertEquals(listOf("Pay 50% deposit"), repository.search("50%").first().map { it.title })
        assertEquals(1, repository.search("%").first().size)
    }

    @Test
    fun settingsStartAtDefaultsAndPersistChanges() = runTest {
        assertEquals(Settings(), settings.current())
        settings.update { it.copy(headsUp = HeadsUp.THIRTY, defaultReminderTime = LocalTime.of(8, 15)) }
        val stored = settings.observe().first()
        assertEquals(HeadsUp.THIRTY, stored.headsUp)
        assertEquals(LocalTime.of(8, 15), stored.defaultReminderTime)
    }

    @Test
    fun reorderStoresTheManualOrder() = runTest {
        val a = repository.addTask(NewTask("A"))
        val b = repository.addTask(NewTask("B"))
        val c = repository.addTask(NewTask("C"))
        repository.reorder(listOf(c.id, a.id, b.id))
        val positions = listOf(c, a, b).map { repository.task(it.id)!!.position }
        assertEquals(listOf(0L, 1L, 2L), positions)
    }

    @Test
    fun everyWriteThatCanMoveAReminderRerunsTheScheduler() = runTest {
        val task = repository.addTask(NewTask("A"))
        repository.updateTask(task.id) { it.copy(title = "B") }
        repository.setCompleted(listOf(task.id), true)
        repository.setDeleted(listOf(task.id), true)
        settings.update { it }
        assertEquals(5, scheduleRuns)
    }

    @Test
    fun subtasksKeepTheirOrder() = runTest {
        val task = repository.addTask(NewTask("Trip"))
        val pack = repository.addSubtask(task.id, "Pack")
        repository.addSubtask(task.id, "Book taxi")
        repository.updateSubtask(pack.copy(done = true))
        val steps = repository.observeSubtasks(task.id).first()
        assertEquals(listOf("Pack" to true, "Book taxi" to false), steps.map { it.title to it.done })
    }
}
