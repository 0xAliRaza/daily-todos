package com.aliraza.dailytodos.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardHoldTest {

    @Test
    fun aPickerOverTheKeyboardHoldsItsHeight() {
        val hold = KeyboardHold()
        hold.pickerOpened(imeBottom = 900)
        assertTrue(hold.holding)
        assertEquals(900, hold.height)
    }

    @Test
    fun withNoKeyboardThereIsNothingToHold() {
        val hold = KeyboardHold()
        hold.pickerOpened(imeBottom = 0)
        assertFalse(hold.holding)
    }

    @Test
    fun aSecondPickerBeforeTheKeyboardIsBackKeepsTheRoom() {
        val hold = KeyboardHold()
        hold.pickerOpened(imeBottom = 900)
        hold.pickerOpened(imeBottom = 0)
        assertTrue(hold.holding)
        assertEquals(900, hold.height)
    }

    @Test
    fun theKeyboardTakesOverOnceItIsAsTallAgain() {
        val hold = KeyboardHold()
        hold.pickerOpened(imeBottom = 900)
        assertFalse(hold.isBack(450))
        assertTrue(hold.isBack(900))
        hold.release()
        assertFalse(hold.holding)
    }
}
