package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SplitSectionTransitionTest {

    @Test
    fun `resolveSplitEditorMode returns SUBUNIT when subunit mode enabled and entity splits non-empty`() {
        val mode = resolveSplitEditorMode(
            isSubunitMode = true,
            hasEntitySplits = true,
            hasFlatSplits = true
        )

        assertEquals(SplitEditorMode.SUBUNIT, mode)
    }

    @Test
    fun `resolveSplitEditorMode falls back to FLAT when subunit mode enabled but entity splits empty`() {
        val mode = resolveSplitEditorMode(
            isSubunitMode = true,
            hasEntitySplits = false,
            hasFlatSplits = true
        )

        assertEquals(SplitEditorMode.FLAT, mode)
    }

    @Test
    fun `resolveSplitEditorMode returns FLAT when subunit mode disabled and flat splits non-empty`() {
        val mode = resolveSplitEditorMode(
            isSubunitMode = false,
            hasEntitySplits = true,
            hasFlatSplits = true
        )

        assertEquals(SplitEditorMode.FLAT, mode)
    }

    @Test
    fun `resolveSplitEditorMode returns NONE when both entity splits and flat splits are empty`() {
        val mode = resolveSplitEditorMode(
            isSubunitMode = true,
            hasEntitySplits = false,
            hasFlatSplits = false
        )

        assertEquals(SplitEditorMode.NONE, mode)
    }
}
