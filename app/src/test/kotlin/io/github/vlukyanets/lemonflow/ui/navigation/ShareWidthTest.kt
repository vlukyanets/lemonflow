package io.github.vlukyanets.lemonflow.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareWidthTest {
    @Test
    fun spareWidthIsSharedEqually() {
        assertEquals(listOf(110, 130, 120, 140), shareWidth(listOf(80, 100, 90, 110), 500))
    }

    @Test
    fun leftoverPixelsGoToTheFirstSlots() {
        val widths = shareWidth(listOf(10, 10, 10), 33)
        assertEquals(listOf(11, 11, 11), widths)
        assertEquals(listOf(12, 11, 11), shareWidth(listOf(10, 10, 10), 34))
    }

    @Test
    fun aLongerLabelGetsAWiderSlot() {
        val widths = shareWidth(listOf(64, 64, 80, 120), 360)
        assertTrue(widths[3] > widths[2] && widths[2] > widths[1])
        assertEquals(360, widths.sum())
    }

    @Test
    fun shortSpaceShrinksInProportionAndStillFillsTheRow() {
        val widths = shareWidth(listOf(100, 200, 100), 300)
        assertEquals(300, widths.sum())
        assertEquals(listOf(75, 150, 75), widths)
    }

    @Test
    fun labelsFitCountsEachTabAtLeastItsMinimum() {
        // Short labels still take the minimum tab width.
        assertTrue(labelsFit(listOf(20, 30, 40), minTab = 56, minSide = 6, available = 168))
        assertFalse(labelsFit(listOf(20, 30, 40), minTab = 56, minSide = 6, available = 167))
        // A long label counts at its own width plus the minimum side padding.
        assertTrue(labelsFit(listOf(60, 120), minTab = 56, minSide = 6, available = 204))
        assertFalse(labelsFit(listOf(60, 120), minTab = 56, minSide = 6, available = 203))
    }

    @Test
    fun emptyAndZeroInputs() {
        assertEquals(emptyList<Int>(), shareWidth(emptyList(), 100))
        assertEquals(listOf(34, 33, 33), shareWidth(listOf(0, 0, 0), 100))
    }
}
