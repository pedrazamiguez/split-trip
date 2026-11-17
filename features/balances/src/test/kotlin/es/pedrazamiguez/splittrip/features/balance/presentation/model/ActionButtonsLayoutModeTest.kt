package es.pedrazamiguez.splittrip.features.balance.presentation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActionButtonsLayoutModeTest {

    @Test
    fun `resolveActionButtonsLayoutMode returns ROW_STANDARD when standard padding fits in half width`() {
        val result = resolveActionButtonsLayoutMode(
            availableWidthPx = 420,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(ActionButtonsLayoutMode.ROW_STANDARD, result)
    }

    @Test
    fun `resolveActionButtonsLayoutMode returns ROW_COMPACT when only compact padding fits in half width`() {
        val result = resolveActionButtonsLayoutMode(
            availableWidthPx = 320,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(ActionButtonsLayoutMode.ROW_COMPACT, result)
    }

    @Test
    fun `resolveActionButtonsLayoutMode returns COLUMN_STACKED when neither fits in half width`() {
        val result = resolveActionButtonsLayoutMode(
            availableWidthPx = 260,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(ActionButtonsLayoutMode.COLUMN_STACKED, result)
    }

    @Test
    fun `resolveActionButtonsLayoutMode returns COLUMN_STACKED when width is at or below spacing`() {
        val equalResult = resolveActionButtonsLayoutMode(
            availableWidthPx = 20,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )
        val smallerResult = resolveActionButtonsLayoutMode(
            availableWidthPx = 10,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 180,
            compactRequiredButtonWidthPx = 140
        )

        assertEquals(ActionButtonsLayoutMode.COLUMN_STACKED, equalResult)
        assertEquals(ActionButtonsLayoutMode.COLUMN_STACKED, smallerResult)
    }

    @Test
    fun `resolveActionButtonsLayoutMode uses inclusive boundary for standard and compact widths`() {
        // halfRowWidthPx = (320 - 20) / 2 = 150
        val exactStandard = resolveActionButtonsLayoutMode(
            availableWidthPx = 320,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 150,
            compactRequiredButtonWidthPx = 120
        )
        val exactCompact = resolveActionButtonsLayoutMode(
            availableWidthPx = 320,
            spacingPx = 20,
            standardRequiredButtonWidthPx = 151,
            compactRequiredButtonWidthPx = 150
        )

        assertEquals(ActionButtonsLayoutMode.ROW_STANDARD, exactStandard)
        assertEquals(ActionButtonsLayoutMode.ROW_COMPACT, exactCompact)
    }
}
