package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReadOnlyFieldSemanticsTest {

    @Test
    fun labelAndValuePresent_joinsWithCommaAndSpace() {
        val result = buildReadOnlyFieldContentDescription(
            label = "Currency",
            value = "EUR (€)",
            placeholder = null
        )

        assertEquals("Currency, EUR (€)", result)
    }

    @Test
    fun labelPresentAndValueBlank_withoutPlaceholder_returnsLabelOnly() {
        val result = buildReadOnlyFieldContentDescription(
            label = "Currency",
            value = "",
            placeholder = null
        )

        assertEquals("Currency", result)
    }

    @Test
    fun labelPresentAndValueBlank_withPlaceholder_joinsLabelAndPlaceholder() {
        val result = buildReadOnlyFieldContentDescription(
            label = "Due date",
            value = "",
            placeholder = "Select date"
        )

        assertEquals("Due date, Select date", result)
    }

    @Test
    fun labelValueAndPlaceholderPresent_prefersValueOverPlaceholder() {
        val result = buildReadOnlyFieldContentDescription(
            label = "Currency",
            value = "USD ($)",
            placeholder = "Select currency"
        )

        assertEquals("Currency, USD ($)", result)
    }

    @Test
    fun labelNullAndValuePresent_returnsValueOnly() {
        val result = buildReadOnlyFieldContentDescription(
            label = null,
            value = "EUR (€)",
            placeholder = null
        )

        assertEquals("EUR (€)", result)
    }

    @Test
    fun labelNullAndValueBlank_withPlaceholder_returnsPlaceholder() {
        val result = buildReadOnlyFieldContentDescription(
            label = null,
            value = "",
            placeholder = "Start date"
        )

        assertEquals("Start date", result)
    }

    @Test
    fun allNullOrBlank_returnsNull() {
        val result = buildReadOnlyFieldContentDescription(
            label = null,
            value = "   ",
            placeholder = "  "
        )

        assertNull(result)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        val result = buildReadOnlyFieldContentDescription(
            label = "  Currency  ",
            value = "  EUR (€)  ",
            placeholder = null
        )

        assertEquals("Currency, EUR (€)", result)
    }
}
