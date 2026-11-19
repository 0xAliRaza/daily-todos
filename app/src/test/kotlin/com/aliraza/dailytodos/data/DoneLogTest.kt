package com.aliraza.dailytodos.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.core.focus.FocusSession
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DoneLogTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = MutableClock(at(today, 9), zone)

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var lists: ListRepository

    private fun at(day: LocalDate, hour: Int): Instant = day.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant()

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val settings = SettingsRepository(db.settings()) {}
        lists = ListRepository(db.lists(), clock) {}
        tasks = TaskRepository(db, clock, settings) {}
    }

    @After
    fun tearDown() = db.close()

    private suspend fun doneAt(task: NewTask, instant: Instant): String {
        val id = tasks.addTask(task).id
        clock.now = instant
        tasks.setCompleted(listOf(id), completed = true)
        return id
    }

    @Test
    fun eachDayHoldsItsDatedDoneTasksWithTheirFocusThatDay() = runTest {
        val report = doneAt(NewTask("Write report", dueDate = today), at(today, 15))
        val call = doneAt(NewTask("Call the plumber", dueDate = today), at(today, 10))
        val milk = lists.create("Groceries").id
        doneAt(NewTask("Milk", listId = milk), at(today, 11))
        val earlier = doneAt(NewTask("Book tickets", dueDate = today), at(today.minusDays(1), 18))
        val focus = Duration.ofMinutes(25)
        db.focusSessions().insert(FocusSession(report, at(today, 13).minus(focus), at(today, 13), focus).toEntity())

        val done = tasks.done.observeBetween(today.minusDays(1), today).first()

        assertEquals(setOf(today, today.minusDays(1)), done.keys)
        // In the order ticked; the undated shopping item stays off the calendar.
        assertEquals(listOf(call, report), done.getValue(today).tasks.map { it.task.id })
        assertEquals(focus, done.getValue(today).focused)
        assertEquals(listOf(earlier), done.getValue(today.minusDays(1)).tasks.map { it.task.id })
        assertEquals(
            emptyMap<LocalDate, Any>(),
            tasks.done.observeBetween(today.plusDays(1), today.plusDays(2)).first(),
        )
    }
}
