package com.aliraza.dailytodos.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.db.AppDatabase
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
    private lateinit var subtasks: SubtaskRepository
    private lateinit var lists: ListRepository
    private lateinit var settings: SettingsRepository
    private var scheduleRuns = 0

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val onScheduleChanged: suspend () -> Unit = { scheduleRuns++ }
        settings = SettingsRepository(db.settings(), onScheduleChanged)
        lists = ListRepository(db.lists(), clock, onScheduleChanged)
        repository = TaskRepository(db, clock, settings, onScheduleChanged)
        subtasks = SubtaskRepository(db.subtasks())
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
    fun doneTodayHoldsWhatTodayShowedAndWasTickedSinceMidnight() = runTest {
        val dueToday = repository.addTask(NewTask("Stretch", dueDate = today))
        val overdue = repository.addTask(NewTask("Pay rent", dueDate = today.minusDays(2)))
        val later = repository.addTask(NewTask("Dentist", dueDate = today.plusDays(3)))
        val groceries = lists.create("Groceries")
        val milk = repository.addTask(NewTask("Milk", listId = groceries.id))
        repository.setCompleted(listOf(dueToday.id, later.id, milk.id), true)
        clock.advance(Duration.ofMinutes(5))
        repository.setCompleted(listOf(overdue.id), true)

        assertEquals(
            listOf("Pay rent", "Stretch"),
            repository.observeCompletedOn(today).first().map { it.title },
        )
        clock.advance(Duration.ofDays(1))
        assertTrue(repository.observeCompletedOn(today.plusDays(1)).first().isEmpty())
    }

    @Test
    fun aListsDoneTasksAreNewestFirstAndLeaveWhenReopened() = runTest {
        val groceries = lists.create("Groceries")
        val milk = repository.addTask(NewTask("Milk", listId = groceries.id))
        val eggs = repository.addTask(NewTask("Eggs", listId = groceries.id))
        repository.setCompleted(listOf(milk.id), true)
        clock.advance(Duration.ofMinutes(1))
        repository.setCompleted(listOf(eggs.id), true)
        assertEquals(listOf("Eggs", "Milk"), repository.observeCompletedInList(groceries.id).first().map { it.title })

        repository.setCompleted(listOf(eggs.id), false)
        assertEquals(listOf("Milk"), repository.observeCompletedInList(groceries.id).first().map { it.title })
    }

    @Test
    fun subtaskProgressCountsTickedAndTotalPerTask() = runTest {
        val trip = repository.addTask(NewTask("Trip"))
        val plain = repository.addTask(NewTask("Plain"))
        subtasks.add(trip.id, "Pack").let { subtasks.update(it.copy(done = true)) }
        subtasks.add(trip.id, "Book taxi")
        subtasks.add(trip.id, "Water plants")
        val progress = subtasks.observeProgress().first()
        assertEquals(mapOf(trip.id to SubtaskProgress(done = 1, total = 3)), progress)
        assertNull(progress[plain.id])
    }

    @Test
    fun openCountsLeaveOutDoneAndDeletedTasks() = runTest {
        val groceries = lists.create("Groceries")
        val milk = repository.addTask(NewTask("Milk", listId = groceries.id))
        repository.addTask(NewTask("Eggs", listId = groceries.id))
        val bread = repository.addTask(NewTask("Bread", listId = groceries.id))
        repository.setCompleted(listOf(milk.id), true)
        repository.setDeleted(listOf(bread.id), true)
        assertEquals(1, repository.observeOpenCounts().first()[groceries.id])
    }

    @Test
    fun aDeletedListHidesItsTasksStopsTheirRemindersAndComesBackOnUndo() = runTest {
        val trip = lists.create("Trip")
        repository.addTask(NewTask("Pack", listId = trip.id, dueDate = today))
        val runsBefore = scheduleRuns
        lists.setDeleted(trip.id, deleted = true)
        assertTrue(repository.openDated().isEmpty())
        assertTrue(lists.observe().first().none { it.id == trip.id })
        assertTrue(scheduleRuns > runsBefore)

        lists.setDeleted(trip.id, deleted = false)
        assertEquals(listOf("Pack"), repository.openDated().map { it.title })
    }

    @Test
    fun theDefaultListCannotBeDeleted() = runTest {
        val daily = lists.observe().first().single { it.isDefault }
        lists.setDeleted(daily.id, deleted = true)
        assertTrue(lists.observe().first().any { it.id == daily.id })
    }

    @Test
    fun purgingADeletedListTakesItsTasksWithIt() = runTest {
        val trip = lists.create("Trip")
        val pack = repository.addTask(NewTask("Pack", listId = trip.id))
        lists.setDeleted(trip.id, deleted = true)
        clock.advance(TaskRepository.UNDO_WINDOW.plusMinutes(1))
        repository.purgeDeleted()
        assertNull(repository.task(pack.id))
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
        subtasks.add(old.id, "Step")
        repository.setDeleted(listOf(old.id), true)

        clock.advance(Duration.ofDays(2))
        val recent = repository.addTask(NewTask("Recent"))
        repository.setDeleted(listOf(recent.id), true)

        repository.purgeDeleted()
        assertNull(repository.task(old.id))
        assertTrue(subtasks.observe(old.id).first().isEmpty())
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
        val pack = subtasks.add(task.id, "Pack")
        subtasks.add(task.id, "Book taxi")
        subtasks.update(pack.copy(done = true))
        val steps = subtasks.observe(task.id).first()
        assertEquals(listOf("Pack" to true, "Book taxi" to false), steps.map { it.title to it.done })
    }
}
