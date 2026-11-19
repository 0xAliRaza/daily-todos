package com.aliraza.dailytasks.core.backup

/** Structural checks on a decoded file: sizes, unique ids and references. Values are checked on mapping. */
internal object BackupValidator {

    fun problem(file: BackupFile): BackupReadResult.Rejected? =
        tooLarge(file) ?: tooLong(file) ?: duplicates(file) ?: brokenReferences(file) ?: defaultList(file)

    private fun tooLarge(file: BackupFile): BackupReadResult.Rejected? {
        val counts = listOf(
            Triple("lists", file.lists.size, Limits.MAX_LISTS),
            Triple("tasks", file.tasks.size, Limits.MAX_TASKS),
            Triple("subtasks", file.subtasks.size, Limits.MAX_SUBTASKS),
            Triple("reminderHistory", file.reminderHistory.size, Limits.MAX_HISTORY),
            Triple("focusSessions", file.focusSessions.size, Limits.MAX_FOCUS_SESSIONS),
        )
        val over = counts.firstOrNull { (_, count, max) -> count > max } ?: return null
        return BackupCodec.reject(RejectReason.TOO_LARGE, over.first)
    }

    /** A text value with its cap; ids, names and titles must also not be blank. */
    private class Text(val field: String, val value: String, val max: Int, val required: Boolean = true) {
        val isBad: Boolean get() = value.length > max || (required && value.isBlank())
    }

    private fun tooLong(file: BackupFile): BackupReadResult.Rejected? {
        val texts = sequence {
            file.lists.forEach { list ->
                yield(Text("lists.id", list.id, Limits.MAX_ID_LENGTH))
                yield(Text("lists.name", list.name, Limits.MAX_NAME_LENGTH))
            }
            file.tasks.forEach { task ->
                yield(Text("tasks.id", task.id, Limits.MAX_ID_LENGTH))
                yield(Text("tasks.title", task.title, Limits.MAX_TITLE_LENGTH))
                yield(Text("tasks.notes", task.notes, Limits.MAX_NOTES_LENGTH, required = false))
            }
            file.subtasks.forEach { subtask ->
                yield(Text("subtasks.id", subtask.id, Limits.MAX_ID_LENGTH))
                yield(Text("subtasks.title", subtask.title, Limits.MAX_TITLE_LENGTH))
            }
        }
        val bad = texts.firstOrNull { it.isBad } ?: return null
        return BackupCodec.reject(RejectReason.INVALID, bad.field)
    }

    private fun duplicates(file: BackupFile): BackupReadResult.Rejected? {
        val tables = mapOf(
            "lists" to file.lists.map { it.id },
            "tasks" to file.tasks.map { it.id },
            "subtasks" to file.subtasks.map { it.id },
        )
        val duplicated = tables.entries.firstOrNull { (_, ids) -> ids.size != ids.toSet().size } ?: return null
        return BackupCodec.reject(RejectReason.INVALID, "duplicate id in ${duplicated.key}")
    }

    private fun brokenReferences(file: BackupFile): BackupReadResult.Rejected? {
        val listIds = file.lists.map { it.id }.toSet()
        val taskIds = file.tasks.map { it.id }.toSet()
        return when {
            file.tasks.any { it.listId !in listIds } -> BackupCodec.reject(RejectReason.INVALID, "tasks.listId")

            file.subtasks.any { it.taskId !in taskIds } -> BackupCodec.reject(RejectReason.INVALID, "subtasks.taskId")

            file.focusSessions.any { it.taskId != null && it.taskId !in taskIds } ->
                BackupCodec.reject(RejectReason.INVALID, "focusSessions.taskId")

            else -> null
        }
    }

    private fun defaultList(file: BackupFile): BackupReadResult.Rejected? =
        if (file.lists.count { it.isDefault } == 1) null else BackupCodec.reject(RejectReason.INVALID, "default list")
}
