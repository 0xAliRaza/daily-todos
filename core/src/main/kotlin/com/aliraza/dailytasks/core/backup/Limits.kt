package com.aliraza.dailytasks.core.backup

/**
 * Caps on an imported file. Far above real use, low enough that a hostile file cannot exhaust memory or
 * fill the database.
 */
object Limits {
    const val MAX_FILE_BYTES = 20L * 1024 * 1024
    const val MAX_LISTS = 1_000
    const val MAX_TASKS = 50_000
    const val MAX_SUBTASKS = 200_000
    const val MAX_HISTORY = 1_000
    const val MAX_ID_LENGTH = 64
    const val MAX_NAME_LENGTH = 200
    const val MAX_TITLE_LENGTH = 2_000
    const val MAX_NOTES_LENGTH = 50_000
}
