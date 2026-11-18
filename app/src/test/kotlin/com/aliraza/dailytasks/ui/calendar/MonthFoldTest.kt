package com.aliraza.dailytasks.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthFoldTest {

    private val max = 200f

    @Test
    fun scrollingUpFoldsTheGridBeforeTheListMoves() {
        val fold = MonthFold()
        assertEquals(-50f, fold.take(-50f, max))
        assertEquals(50f, fold.folded)
    }

    @Test
    fun theGridFoldsNoFurtherThanOneWeekAndPassesTheRestOn() {
        val fold = MonthFold()
        assertEquals(-200f, fold.take(-260f, max))
        assertEquals(max, fold.folded)
        assertEquals(0f, fold.take(-10f, max))
    }

    @Test
    fun pullingDownUnfoldsItAgain() {
        val fold = MonthFold()
        fold.take(-120f, max)
        assertEquals(120f, fold.take(300f, max))
        assertEquals(0f, fold.folded)
    }

    @Test
    fun aShorterMonthKeepsTheFoldInsideIt() {
        val fold = MonthFold()
        fold.take(-200f, max)
        assertEquals(-0f, fold.take(0f, 150f), 0f)
        assertEquals(150f, fold.folded)
    }

    @Test
    fun aLetGoFoldSettlesToTheNearerEnd() {
        val fold = MonthFold()
        fold.take(-120f, max)
        assertEquals(max, fold.settleTarget(max))
        fold.take(60f, max)
        assertEquals(0f, fold.settleTarget(max))
    }
}
