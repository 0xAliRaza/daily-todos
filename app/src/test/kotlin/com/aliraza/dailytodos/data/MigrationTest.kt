package com.aliraza.dailytodos.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.Migrations
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalTime

/**
 * Upgrading the app keeps every list, task and setting. The old database is built from its exported
 * schema; opening it with the current Room version runs the migration and checks the result against the
 * current schema, table by table.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    /** Creates the database exactly as version [version] of the app did, then fills it with [fill]. */
    private fun createVersion(version: Int, fill: (SupportSQLiteDatabase) -> Unit) {
        val schema = JSONObject(File(SCHEMAS, "$version.json").readText()).getJSONObject("database")
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: continue
                        for (j in 0 until indices.length()) {
                            db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                        }
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.use(fill)
    }

    @Test
    fun versionOneUpgradesWithEverythingKept() = runTest {
        createVersion(1) { db ->
            db.execSQL("INSERT INTO lists (id, name, position, is_default) VALUES ('default', 'Daily', 0, 1)")
            db.execSQL(
                "INSERT INTO tasks (id, list_id, title, notes, priority, position, created_at, updated_at) " +
                    "VALUES ('t1', 'default', 'Call bank', '', 1, 0, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO settings VALUES (1, '08:30', 0, 1, 10, 1, '07:00', 1, 50, 10, 0, 'VIBRATE', 'DARK', NULL)",
            )
            db.execSQL("INSERT INTO timer_state VALUES (1, 'WAITING', NULL, NULL, NULL, 3)")
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*Migrations.ALL).build()
        try {
            assertEquals("Call bank", room.tasks().get("t1")?.title)
            val settings = room.settings().get()!!
            assertEquals(LocalTime.of(8, 30), settings.defaultReminderTime)
            assertEquals(50, settings.focusMinutes)
            assertEquals(10, settings.breakMinutes)
            assertEquals("VIBRATE", settings.timerSignal)
            assertEquals("DARK", settings.theme)
            assertEquals(Settings.DEFAULT_FOCUS_CUSTOM_MINUTES, settings.focusCustomMinutes)
            assertEquals(Settings.DEFAULT_BREAK_CUSTOM_MINUTES, settings.breakCustomMinutes)
            // A timer from before the upgrade is dropped; there is no focus recorded yet.
            assertNull(room.timer().get())
            assertTrue(room.focusSessions().all().isEmpty())
        } finally {
            room.close()
        }
    }

    @Test
    fun versionTwoUpgradesWithEverythingKept() = runTest {
        createVersion(2) { db ->
            db.execSQL("INSERT INTO lists (id, name, position, is_default) VALUES ('default', 'Daily', 0, 1)")
            db.execSQL(
                "INSERT INTO tasks (id, list_id, title, notes, priority, position, created_at, updated_at) " +
                    "VALUES ('t1', 'default', 'Call bank', '', 1, 0, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO settings VALUES (1, '08:30', 0, 1, 10, 1, '07:00', 1, 50, 10, 'VIBRATE', 'DARK', NULL)",
            )
            db.execSQL("INSERT INTO timer_state VALUES (1, 'FOCUS', 1500000, 't1', NULL, 1500000)")
            db.execSQL(
                "INSERT INTO focus_sessions (task_id, started_at, ended_at, focused_ms) " +
                    "VALUES ('t1', 0, 1500000, 1500000)",
            )
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*Migrations.ALL).build()
        try {
            assertEquals("Call bank", room.tasks().get("t1")?.title)
            val settings = room.settings().get()!!
            assertEquals(LocalTime.of(8, 30), settings.defaultReminderTime)
            assertEquals(50, settings.focusMinutes)
            assertEquals(10, settings.breakMinutes)
            assertEquals(Settings.DEFAULT_FOCUS_CUSTOM_MINUTES, settings.focusCustomMinutes)
            assertEquals(Settings.DEFAULT_BREAK_CUSTOM_MINUTES, settings.breakCustomMinutes)
            assertEquals("VIBRATE", settings.timerSignal)
            // A running timer and the focus kept so far survive this upgrade.
            assertEquals("t1", room.timer().get()?.taskId)
            assertEquals(listOf(1_500_000L), room.focusSessions().all().map { it.focusedMs })
        } finally {
            room.close()
        }
    }

    private companion object {
        /** Unit tests run in the module directory. */
        val SCHEMAS = File("schemas/com.aliraza.dailytodos.data.db.AppDatabase")
    }
}
