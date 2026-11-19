package com.aliraza.dailytasks.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleHeadroomTest {
    @Test
    fun aShortScreenGetsNoExtraSpace() {
        assertEquals(0.dp, titleHeadroom(560.dp))
        assertEquals(0.dp, titleHeadroom(640.dp))
    }

    @Test
    fun aTallerScreenGetsMoreUpToTheCap() {
        val phone = titleHeadroom(832.dp)
        assertTrue("$phone", phone > 24.dp && phone < 40.dp)
        assertTrue(titleHeadroom(900.dp) > phone)
        assertEquals(48.dp, titleHeadroom(1400.dp))
    }
}
