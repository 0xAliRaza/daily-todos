package com.aliraza.dailytodos.ui.settings

import android.Manifest
import android.app.Application
import android.os.PowerManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.MutableClock
import com.aliraza.dailytodos.TestViewModels
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.ZoneId

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
            )
        }
    }

    /** The ViewModel's reads stop before the database closes under them. */
    @After
    fun tearDown() {
        viewModels.clear()
        db.close()
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

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
