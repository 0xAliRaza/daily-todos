package com.aliraza.dailytodos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aliraza.dailytodos.core.model.TaskList

@Database(
    entities = [
        ListEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        ReminderLogEntity::class,
        SettingsEntity::class,
        TimerStateEntity::class,
        FocusSessionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao

    abstract fun lists(): ListDao

    abstract fun subtasks(): SubtaskDao

    abstract fun settings(): SettingsDao

    abstract fun reminderLog(): ReminderLogDao

    abstract fun timer(): TimerDao

    abstract fun focusSessions(): FocusSessionDao

    companion object {
        const val FILE_NAME = "daily-todos.db"
        const val DEFAULT_LIST_ID = "default"

        fun build(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME)
            .addMigrations(*Migrations.ALL)
            .seeded()
            .build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).seeded().build()

        private fun Builder<AppDatabase>.seeded() = addCallback(SeedCallback)
    }

    /**
     * First run: the default "Daily" list. The settings row is written on the first change; until then
     * a missing row means every setting is at its default.
     */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO lists (id, name, position, is_default) VALUES (?, ?, 0, 1)",
                arrayOf(DEFAULT_LIST_ID, TaskList.DEFAULT_NAME),
            )
        }
    }
}
