package com.aliraza.dailytodos.ui.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialInputTest {

    private val focus = 1..180
    private val delta = 1e-3f

    @Test
    fun anglesRunClockwiseFromTwelveOClock() {
        assertEquals(0f, dialDegrees(0f, -10f), delta)
        assertEquals(90f, dialDegrees(10f, 0f), delta)
        assertEquals(180f, dialDegrees(0f, 10f), delta)
        assertEquals(270f, dialDegrees(-10f, 0f), delta)
    }

    @Test
    fun anglesMapOntoTheHourFace() {
        assertEquals(15, minutesAt(90f, focus))
        assertEquals(30, minutesAt(180f, focus))
        assertEquals(60, minutesAt(360f, focus))
        assertEquals(75, minutesAt(450f, focus))
    }

    @Test
    fun minutesSnapToTheNearestWholeMinute() {
        assertEquals(25, minutesAt(151f, focus))
        assertEquals(26, minutesAt(153.5f, focus))
    }

    @Test
    fun minutesStayInsideTheRange() {
        assertEquals(1, minutesAt(0f, focus))
        assertEquals(1, minutesAt(-40f, focus))
        assertEquals(180, minutesAt(1500f, focus))
        assertEquals(60, minutesAt(400f, 1..60))
    }

    @Test
    fun theAngleInvertsWhatTheRingDraws() {
        for (m in 1..60) {
            val sweep = DialReading(minutes(m), minutes(m)).sweep
            assertEquals(m, minutesAt(sweep * 360f, focus))
        }
    }

    @Test
    fun windingPastTwelveKeepsCounting() {
        assertEquals(370f, unwrapAngle(350f, 10f), delta)
        assertEquals(730f, unwrapAngle(710f, 10f), delta)
        assertEquals(-10f, unwrapAngle(10f, 350f), delta)
        assertEquals(380f, unwrapAngle(370f, 20f), delta)
        assertEquals(355f, unwrapAngle(370f, 355f), delta)
    }

    @Test
    fun aTouchStartsOnTheHourOfTheCurrentLength() {
        assertEquals(90f, touchAngle(90f, 25), delta)
        assertEquals(90f, touchAngle(90f, 60), delta)
        assertEquals(450f, touchAngle(90f, 90), delta)
        assertEquals(810f, touchAngle(90f, 150), delta)
    }

    @Test
    fun aTickMarksEachStepReachedEitherWay() {
        assertTrue(passesMark(4, 5, 5))
        assertTrue(passesMark(9, 11, 5))
        assertFalse(passesMark(5, 6, 5))
        assertTrue(passesMark(6, 5, 5))
        assertFalse(passesMark(10, 9, 5))
        assertTrue(passesMark(7, 8, 1))
        assertFalse(passesMark(7, 7, 1))
    }
}
