package com.aliraza.dailytodos.core.model

/** Optional task priority. [P1] is the most urgent; [level] is what the database stores. */
enum class Priority(val level: Int) {
    NONE(0),
    P1(1),
    P2(2),
    P3(3),
    ;

    companion object {
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}
