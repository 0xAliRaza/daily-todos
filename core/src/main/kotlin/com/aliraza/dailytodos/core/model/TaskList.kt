package com.aliraza.dailytodos.core.model

/** A named group of tasks. Exactly one list is the default ("Daily"); it can be renamed, never deleted. */
data class TaskList(val id: String, val name: String, val position: Long, val isDefault: Boolean) {
    companion object {
        const val DEFAULT_NAME = "Daily"
    }
}
