package com.aliraza.dailytodos.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The large header folds from a 56 row over the title block into a 64 bar as the page scrolls, and
 * unfolds only once the page is back at its top. Sizes are the Tokens board's: 56 + 38 + 20 + 12 open,
 * 64 folded.
 */
class HeaderCollapseTest {
    private val row = 56f
    private val bar = 64f

    /** Title 38, date 2 + 20, 12 under it. */
    private val block = 72f

    @Test
    fun openItIsTheRowOverTheWholeTitle() {
        val frame = headerFrame(row, bar, block, offset = 0f)
        assertEquals(128f, frame.height)
        assertEquals(row, frame.row)
        assertEquals(block, frame.block)
        assertEquals(1f, frame.titleAlpha)
        assertEquals(0f, frame.barTitleAlpha)
    }

    @Test
    fun foldedItIsTheBarAloneWithItsSmallTitle() {
        val range = headerRange(row, bar, block)
        assertEquals(64f, range)
        val frame = headerFrame(row, bar, block, offset = range)
        assertEquals(bar, frame.height)
        assertEquals(bar, frame.row)
        assertEquals(0f, frame.block)
        assertEquals(0f, frame.titleAlpha)
        assertEquals(1f, frame.barTitleAlpha)
    }

    @Test
    fun halfwayTheHeightFallsEvenlyAndNeitherTitleShows() {
        val frame = headerFrame(row, bar, block, offset = 32f)
        assertEquals(96f, frame.height)
        assertEquals(60f, frame.row)
        assertEquals(36f, frame.block)
        assertEquals(0f, frame.titleAlpha)
        assertEquals(0f, frame.barTitleAlpha)
    }

    @Test
    fun aHeaderWithNoTitleBlockDoesNotFold() {
        assertEquals(0f, headerRange(row, bar, block = 0f))
        assertEquals(row, headerFrame(row, bar, block = 0f, offset = 40f).height)
    }

    @Test
    fun foldingStopsAtEitherEnd() {
        val collapse = sized()
        assertEquals(50f, collapse.fold(50f))
        assertEquals(14f, collapse.fold(50f))
        assertEquals(1f, collapse.fraction)
        assertEquals(-64f, collapse.fold(-100f))
        assertEquals(0f, collapse.offset)
    }

    @Test
    fun scrollingUpFoldsTheHeaderBeforeThePageMoves() {
        val collapse = sized()
        val used = collapse.connection.onPreScroll(Offset(0f, -100f), NestedScrollSource.UserInput)
        // The header takes its 64 px; the page scrolls the other 36.
        assertEquals(-64f, used.y)
        assertEquals(64f, collapse.offset)
    }

    @Test
    fun scrollingDownMovesThePageFirstAndUnfoldsOnlyAtItsTop() {
        val collapse = sized()
        collapse.fold(64f)
        val before = collapse.connection.onPreScroll(Offset(0f, 30f), NestedScrollSource.UserInput)
        assertEquals(0f, before.y)
        assertEquals(64f, collapse.offset)
        // The page at its top hands back what it could not scroll; the header takes it.
        val after = collapse.connection.onPostScroll(Offset.Zero, Offset(0f, 30f), NestedScrollSource.UserInput)
        assertEquals(30f, after.y)
        assertEquals(34f, collapse.offset)
    }

    @Test
    fun aSmallerRangeKeepsTheFoldInsideIt() {
        val collapse = sized()
        collapse.fold(64f)
        collapse.fitRange(20f)
        assertEquals(20f, collapse.offset)
    }

    private fun sized() = HeaderCollapse().apply { fitRange(headerRange(row, bar, block)) }
}
