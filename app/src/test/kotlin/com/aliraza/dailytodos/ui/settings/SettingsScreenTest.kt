package com.aliraza.dailytodos.ui.settings

import android.Manifest
import android.app.Application
import android.os.PowerManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.core.model.Accent
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.ReminderLogEntity
import com.aliraza.dailytodos.reminder.TestReminder
import com.aliraza.dailytodos.ui.components.phoneFirstDay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** The settings screen reads live data: the default list's name and the reminder health summary. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val clock = MutableClock(Instant.parse("2026-11-18T11:00:00Z"), ZoneId.of("Europe/Zurich"))
    private val db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
    private val lists = ListRepository(db.lists(), clock)
    private val viewModels = TestViewModels()

    private fun viewModel(): SettingsViewModel {
        val settings = SettingsRepository(db.settings()) {}
        return viewModels.add {
            SettingsViewModel(
                settings,
                BackupRepository(db, settings, clock) {},
                TaskRepository(db, clock, settings) {},
                lists,
                db.reminderLog(),
                clock,
                testReminder = {},
            )
        }
    }

    /** The ViewModel's reads stop before the database closes under them. */
    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
    }

    /** Waits for [condition], idling the main looper between checks so the ViewModel's work can run. */
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(TIMEOUT_MS) {
        compose.waitForIdle()
        condition()
    }

    private fun waitForText(text: String) =
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun theTomorrowRowNamesTheDefaultListAsItIsNow() {
        val vm = viewModel()
        compose.setContent { SettingsScreen(vm, PaddingValues(), onHealth = {}, onFocusStats = {}) }
        waitForText("New Daily tasks get tomorrow")

        runBlocking { lists.rename(AppDatabase.DEFAULT_LIST_ID, "Home") }

        waitForText("New Home tasks get tomorrow")
    }

    /** Passes the exact alarm and battery checks, and the notifications check when [notifications]. */
    private fun passChecks(notifications: Boolean) {
        val app: Application = ApplicationProvider.getApplicationContext()
        if (notifications) shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(app.getSystemService(PowerManager::class.java)).setIgnoringBatteryOptimizations(app.packageName, true)
    }

    @Test
    fun reminderHealthSaysAllSetWhenEveryCheckPasses() {
        passChecks(notifications = true)
        val vm = viewModel()
        compose.setContent { SettingsScreen(vm, PaddingValues(), onHealth = {}, onFocusStats = {}) }
        waitForText("All set")
    }

    @Test
    fun reminderHealthCountsWhatIsLeftToFix() {
        passChecks(notifications = false)
        val vm = viewModel()
        compose.setContent { SettingsScreen(vm, PaddingValues(), onHealth = {}, onFocusStats = {}) }
        waitForText("1 thing to fix")
    }

    private fun showSettings(vm: SettingsViewModel) =
        compose.setContent { SettingsScreen(vm, PaddingValues(), onHealth = {}, onFocusStats = {}) }

    @Test
    fun weekStartOffersPhoneMondayAndSundayWithThePhonesDayInTheHint() {
        showSettings(viewModel())
        val phoneDay = phoneFirstDay().getDisplayName(TextStyle.FULL, Locale.getDefault())
        waitForText("Your phone starts weeks on $phoneDay")
        listOf("Phone", "Monday", "Sunday").forEach { label ->
            compose.onAllNodesWithText(label).assertCountEquals(1)
        }
    }

    @Test
    fun blackBackgroundIsGreyedOutInTheLightThemeOnly() {
        val vm = viewModel()
        vm.update { it.copy(theme = ThemeMode.LIGHT) }
        waitFor { vm.settings.value.theme == ThemeMode.LIGHT }
        showSettings(vm)
        compose.onNodeWithText("Black background").assertIsNotEnabled()

        vm.update { it.copy(theme = ThemeMode.DARK) }
        waitFor { vm.settings.value.theme == ThemeMode.DARK }
        compose.onNodeWithText("Black background").assertIsEnabled().performScrollTo().performClick()
        waitFor { vm.settings.value.blackBackground }
    }

    @Test
    fun pickingAnAccentSavesItAndMarksItPicked() {
        val vm = viewModel()
        showSettings(vm)
        compose.onNodeWithContentDescription("Ink blue").assertIsSelected()

        compose.onNodeWithContentDescription("Violet").performScrollTo().performClick()

        waitFor { vm.settings.value.accent == Accent.VIOLET }
        compose.onNodeWithContentDescription("Violet").assertIsSelected()
        compose.onNodeWithContentDescription("Ink blue").assertIsNotSelected()
    }

    @Test
    fun historyNamesEachReminderFromTheLiveTitles() {
        val vm = viewModel()
        val tasks = TaskRepository(db, clock, SettingsRepository(db.settings()) {}) {}
        val at = clock.instant()
        // Only the writes block, on Room's own threads; the history is read on the main looper below.
        val kept = runBlocking { tasks.addTask(NewTask(title = "Call bank")) }
        runBlocking {
            listOf(kept.id, "gone", TestReminder.TASK_ID).forEach { id ->
                db.reminderLog().insert(
                    ReminderLogEntity(taskId = id, dueAt = at, firedAt = at, firedBy = FiredBy.ALARM),
                )
            }
        }
        var history = emptyList<HistoryItem>()
        compose.setContent { history = vm.history.collectAsState().value }
        waitFor { history.size == 3 }

        val titles = history.associate { it.entry.taskId to it.title }
        assertEquals(HistoryTitle.Task("Call bank"), titles[kept.id])
        assertEquals(HistoryTitle.Deleted, titles["gone"])
        assertEquals(HistoryTitle.Test, titles[TestReminder.TASK_ID])
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
