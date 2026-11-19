package com.aliraza.dailytodos.core.model

/** One checkbox inside a task. Sub-tasks have no sub-tasks of their own. */
data class Subtask(val id: String, val taskId: String, val title: String, val done: Boolean, val position: Long)
