package es.pedrazamiguez.splittrip.features.settlement.presentation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementCardActionLayoutModeTest {

    @Test
    fun `SettlementCardAction defaults isEnabled to true and invokes onClick`() {
        var clicked = false
        val defaultAction = SettlementCardAction(
            label = "Review",
            isPrimary = false,
            onClick = { clicked = true }
        )
        val disabledAction = SettlementCardAction(
            label = "Confirm",
            isPrimary = true,
            isEnabled = false,
            onClick = {}
        )

        assertEquals("Review", defaultAction.label)
        assertFalse(defaultAction.isPrimary)
        assertTrue(defaultAction.isEnabled)
        defaultAction.onClick()
        assertTrue(clicked)

        assertEquals("Confirm", disabledAction.label)
        assertTrue(disabledAction.isPrimary)
        assertFalse(disabledAction.isEnabled)
    }

    @Test
    fun `returns ROW_STANDARD when actionCount is 1 regardless of narrow width`() {
        val result = resolveSettlementCardActionLayoutMode(
            actionCount = 1,
            availableWidthPx = 100,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 120,
            compactRequiredButtonWidthPx = 90
        )

        assertEquals(SettlementCardActionLayoutMode.ROW_STANDARD, result)
    }

    @Test
    fun `returns ROW_STANDARD when 2 buttons with standard padding fit in slot width`() {
        val result = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 420,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(SettlementCardActionLayoutMode.ROW_STANDARD, result)
    }

    @Test
    fun `returns ROW_COMPACT when 2 buttons only fit with compact padding`() {
        val result = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 320,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(SettlementCardActionLayoutMode.ROW_COMPACT, result)
    }

    @Test
    fun `returns COLUMN_STACKED when 2 buttons exceed slot width even with compact padding`() {
        val result = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 260,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(SettlementCardActionLayoutMode.COLUMN_STACKED, result)
    }

    @Test
    fun `handles 3 actions across ROW_STANDARD, ROW_COMPACT, and COLUMN_STACKED`() {
        // totalSpacingPx = 16 * 2 = 32
        // availableWidthPx = 602 -> slotWidthPx = (602 - 32) / 3 = 190
        val standardResult = resolveSettlementCardActionLayoutMode(
            actionCount = 3,
            availableWidthPx = 602,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )
        // availableWidthPx = 482 -> slotWidthPx = (482 - 32) / 3 = 150
        val compactResult = resolveSettlementCardActionLayoutMode(
            actionCount = 3,
            availableWidthPx = 482,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )
        // availableWidthPx = 422 -> slotWidthPx = (422 - 32) / 3 = 130
        val stackedResult = resolveSettlementCardActionLayoutMode(
            actionCount = 3,
            availableWidthPx = 422,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(SettlementCardActionLayoutMode.ROW_STANDARD, standardResult)
        assertEquals(SettlementCardActionLayoutMode.ROW_COMPACT, compactResult)
        assertEquals(SettlementCardActionLayoutMode.COLUMN_STACKED, stackedResult)
    }

    @Test
    fun `returns COLUMN_STACKED when available width is at or below total spacing`() {
        val equalResult = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 16,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )
        val smallerResult = resolveSettlementCardActionLayoutMode(
            actionCount = 3,
            availableWidthPx = 20,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(SettlementCardActionLayoutMode.COLUMN_STACKED, equalResult)
        assertEquals(SettlementCardActionLayoutMode.COLUMN_STACKED, smallerResult)
    }

    @Test
    fun `uses inclusive boundary for standard and compact widths`() {
        // slotWidthPx = (316 - 16) / 2 = 150
        val exactStandard = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 316,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 150,
            compactRequiredButtonWidthPx = 120
        )
        val exactCompact = resolveSettlementCardActionLayoutMode(
            actionCount = 2,
            availableWidthPx = 316,
            spacingPx = 16,
            standardRequiredButtonWidthPx = 151,
            compactRequiredButtonWidthPx = 150
        )

        assertEquals(SettlementCardActionLayoutMode.ROW_STANDARD, exactStandard)
        assertEquals(SettlementCardActionLayoutMode.ROW_COMPACT, exactCompact)
    }
}
