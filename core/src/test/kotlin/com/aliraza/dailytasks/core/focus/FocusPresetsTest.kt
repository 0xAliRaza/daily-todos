package com.aliraza.dailytasks.core.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusPresetsTest {
    @Test
    fun aLengthPickedByHandBecomesTheFourthChip() {
        assertEquals(35, FocusPresets.customAfter(FocusPresets.FOCUS, custom = 90, picked = 35))
        assertEquals(7, FocusPresets.customAfter(FocusPresets.BREAK, custom = 20, picked = 7))
    }

    @Test
    fun steppingOntoAPresetKeepsTheFourthChip() {
        FocusPresets.FOCUS.forEach { assertEquals(35, FocusPresets.customAfter(FocusPresets.FOCUS, 35, it)) }
        FocusPresets.BREAK.forEach { assertEquals(7, FocusPresets.customAfter(FocusPresets.BREAK, 7, it)) }
    }

    @Test
    fun tappingTheFourthChipKeepsIt() {
        assertEquals(90, FocusPresets.customAfter(FocusPresets.FOCUS, custom = 90, picked = 90))
    }
}
