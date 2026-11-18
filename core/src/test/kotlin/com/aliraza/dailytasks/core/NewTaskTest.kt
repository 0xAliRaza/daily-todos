package com.aliraza.dailytasks.core

import com.aliraza.dailytasks.core.model.NewTask
import com.aliraza.dailytasks.core.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NewTaskTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val settings = Settings()

    @Test
    fun undatedTaskInTheDefaultListGetsTomorrow() {
        assertEquals(today.plusDays(1), NewTask("Milk").effectiveDueDate(true, settings, today))
    }

    @Test
    fun undatedTaskInAnotherListStaysUndated() {
        assertNull(NewTask("Milk").effectiveDueDate(false, settings, today))
    }

    @Test
    fun tappedAwayChipOrSwitchedOffSettingKeepsItUndated() {
        assertNull(NewTask("Milk", useDefaultDate = false).effectiveDueDate(true, settings, today))
        assertNull(NewTask("Milk").effectiveDueDate(true, settings.copy(undatedGetsTomorrow = false), today))
    }

    @Test
    fun typedDateAlwaysWins() {
        val friday = LocalDate.of(2026, 11, 20)
        assertEquals(friday, NewTask("Milk", dueDate = friday).effectiveDueDate(false, settings, today))
    }
}
