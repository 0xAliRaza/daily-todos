package com.aliraza.dailytasks.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schema upgrades. Each one keeps every task, list and setting; schemas/ holds every version. */
object Migrations {
    /**
     * Version 2: focus belongs to a task. Adds the focus sessions, drops the "breaks" switch from settings
     * and gives the timer its round length. A timer running during the upgrade is dropped: SQLite before
     * Android 12 cannot drop a column, and a lost countdown costs one tap.
     */
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`task_id` TEXT, `started_at` INTEGER NOT NULL, `ended_at` INTEGER NOT NULL, " +
                    "`focused_ms` INTEGER NOT NULL, FOREIGN KEY(`task_id`) REFERENCES `tasks`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_task_id` ON `focus_sessions` (`task_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_ended_at` ON `focus_sessions` (`ended_at`)")

            val kept = "`id`, `default_reminder_time`, `undated_gets_tomorrow`, `date_only_gets_reminder`, " +
                "`heads_up_minutes`, `overdue_reminders`, `overdue_reminder_time`, `dnd_override`, " +
                "`focus_minutes`, `break_minutes`, `timer_signal`, `theme`, `last_backup_at`"
            db.execSQL(
                "CREATE TABLE `settings_new` (`id` INTEGER NOT NULL, `default_reminder_time` TEXT NOT NULL, " +
                    "`undated_gets_tomorrow` INTEGER NOT NULL, `date_only_gets_reminder` INTEGER NOT NULL, " +
                    "`heads_up_minutes` INTEGER NOT NULL, `overdue_reminders` INTEGER NOT NULL, " +
                    "`overdue_reminder_time` TEXT NOT NULL, `dnd_override` INTEGER NOT NULL, " +
                    "`focus_minutes` INTEGER NOT NULL, `break_minutes` INTEGER NOT NULL, " +
                    "`timer_signal` TEXT NOT NULL, `theme` TEXT NOT NULL, `last_backup_at` INTEGER, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL("INSERT INTO `settings_new` ($kept) SELECT $kept FROM `settings`")
            db.execSQL("DROP TABLE `settings`")
            db.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")

            db.execSQL("DROP TABLE `timer_state`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `timer_state` (`id` INTEGER NOT NULL, `phase` TEXT NOT NULL, " +
                    "`ends_at` INTEGER, `task_id` TEXT, `paused_remaining_ms` INTEGER, `round_ms` INTEGER, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

    /** Version 3: the timer's fourth chip, the last focus and break lengths picked by hand. */
    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `settings` ADD COLUMN `focus_custom_minutes` INTEGER NOT NULL DEFAULT 90")
            db.execSQL("ALTER TABLE `settings` ADD COLUMN `break_custom_minutes` INTEGER NOT NULL DEFAULT 20")
        }
    }

    val ALL = arrayOf(V1_TO_V2, V2_TO_V3)
}
