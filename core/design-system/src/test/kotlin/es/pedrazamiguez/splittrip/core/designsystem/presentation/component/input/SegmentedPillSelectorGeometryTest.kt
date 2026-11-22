package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SegmentedPillSelectorGeometry")
class SegmentedPillSelectorGeometryTest {

    private data class OptionItem(val id: String, val label: String)

    @Nested
    @DisplayName("resolveSelectedOptionIndex()")
    inner class ResolveSelectedOptionIndex {

        @Test
        @DisplayName("returns NO_SELECTION_INDEX when options list is empty")
        fun `resolveSelectedOptionIndex returns NO_SELECTION_INDEX when options list is empty`() {
            val result = resolveSelectedOptionIndex(
                options = emptyList<String>(),
                selectedOption = "EQUAL",
                optionKey = { it }
            )

            assertEquals(NO_SELECTION_INDEX, result)
        }

        @Test
        @DisplayName("returns NO_SELECTION_INDEX when selectedOption is null")
        fun `resolveSelectedOptionIndex returns NO_SELECTION_INDEX when selectedOption is null`() {
            val result = resolveSelectedOptionIndex(
                options = listOf("EQUAL", "EXACT", "PERCENT"),
                selectedOption = null,
                optionKey = { it }
            )

            assertEquals(NO_SELECTION_INDEX, result)
        }

        @Test
        @DisplayName("returns NO_SELECTION_INDEX when selectedOption key does not match any option")
        fun `resolveSelectedOptionIndex returns NO_SELECTION_INDEX when selectedOption key does not match`() {
            val result = resolveSelectedOptionIndex(
                options = listOf("EQUAL", "EXACT"),
                selectedOption = "PERCENT",
                optionKey = { it }
            )

            assertEquals(NO_SELECTION_INDEX, result)
        }

        @Test
        @DisplayName("matches option using custom optionKey")
        fun `resolveSelectedOptionIndex matches option using custom optionKey`() {
            val options = listOf(
                OptionItem(id = "EQUAL", label = "Equal"),
                OptionItem(id = "EXACT", label = "Exact"),
                OptionItem(id = "PERCENT", label = "Percent")
            )
            val selected = OptionItem(id = "EXACT", label = "Custom Label")

            val result = resolveSelectedOptionIndex(
                options = options,
                selectedOption = selected,
                optionKey = { it.id }
            )

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("calculateSegmentSlotWidth()")
    inner class CalculateSegmentSlotWidth {

        @Test
        @DisplayName("returns 0dp when optionCount is zero or negative")
        fun `calculateSegmentSlotWidth returns 0dp when optionCount is zero or negative`() {
            assertEquals(0.dp, calculateSegmentSlotWidth(containerWidth = 300.dp, optionCount = 0))
            assertEquals(0.dp, calculateSegmentSlotWidth(containerWidth = 300.dp, optionCount = -2))
        }

        @Test
        @DisplayName("returns 0dp when containerWidth is zero or negative")
        fun `calculateSegmentSlotWidth returns 0dp when containerWidth is zero or negative`() {
            assertEquals(0.dp, calculateSegmentSlotWidth(containerWidth = 0.dp, optionCount = 3))
            assertEquals(0.dp, calculateSegmentSlotWidth(containerWidth = (-10).dp, optionCount = 3))
        }

        @Test
        @DisplayName("divides container width evenly across options")
        fun `calculateSegmentSlotWidth divides container width evenly across options`() {
            val result = calculateSegmentSlotWidth(containerWidth = 300.dp, optionCount = 3)

            assertEquals(100.dp, result)
        }
    }

    @Nested
    @DisplayName("calculateSegmentIndicatorOffset()")
    inner class CalculateSegmentIndicatorOffset {

        @Test
        @DisplayName("returns 0dp for index zero or negative")
        fun `calculateSegmentIndicatorOffset returns 0dp for index zero or negative`() {
            assertEquals(0.dp, calculateSegmentIndicatorOffset(selectedIndex = 0, slotWidth = 100.dp))
            assertEquals(0.dp, calculateSegmentIndicatorOffset(selectedIndex = -1, slotWidth = 100.dp))
        }

        @Test
        @DisplayName("returns 0dp when slotWidth is zero or negative")
        fun `calculateSegmentIndicatorOffset returns 0dp when slotWidth is zero or negative`() {
            assertEquals(0.dp, calculateSegmentIndicatorOffset(selectedIndex = 2, slotWidth = 0.dp))
            assertEquals(0.dp, calculateSegmentIndicatorOffset(selectedIndex = 2, slotWidth = (-10).dp))
        }

        @Test
        @DisplayName("multiplies selectedIndex by slotWidth")
        fun `calculateSegmentIndicatorOffset multiplies selectedIndex by slotWidth`() {
            val result = calculateSegmentIndicatorOffset(selectedIndex = 2, slotWidth = 85.dp)

            assertEquals(170.dp, result)
        }
    }

    @Nested
    @DisplayName("isCompactSegmentSlot()")
    inner class IsCompactSegmentSlot {

        @Test
        @DisplayName("returns true below threshold and false at or above threshold")
        fun `isCompactSegmentSlot returns true below threshold and false at or above threshold`() {
            assertTrue(isCompactSegmentSlot(slotWidth = 99.dp))
            assertFalse(isCompactSegmentSlot(slotWidth = 100.dp))
            assertFalse(isCompactSegmentSlot(slotWidth = 120.dp))
        }
    }
}
