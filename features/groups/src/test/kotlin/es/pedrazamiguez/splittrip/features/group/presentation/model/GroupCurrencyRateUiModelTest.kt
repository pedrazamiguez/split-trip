package es.pedrazamiguez.splittrip.features.group.presentation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupCurrencyRateUiModelTest {

    @Test
    fun `default formattedRate is null`() {
        val model = GroupCurrencyRateUiModel(currency = "USD")

        assertEquals("USD", model.currency)
        assertNull(model.formattedRate)
    }

    @Test
    fun `custom formattedRate is preserved`() {
        val model = GroupCurrencyRateUiModel(currency = "EUR", formattedRate = "1 USD ≈ 0.92 EUR")

        assertEquals("EUR", model.currency)
        assertEquals("1 USD ≈ 0.92 EUR", model.formattedRate)
    }

    @Test
    fun `data class copy and equality work as expected`() {
        val model1 = GroupCurrencyRateUiModel(currency = "USD", formattedRate = "1 EUR ≈ 1.08 USD")
        val model2 = model1.copy(formattedRate = "1 EUR ≈ 1.09 USD")
        val model3 = model1.copy()

        assertEquals(model1, model3)
        assertNotEquals(model1, model2)
        assertEquals(model1.hashCode(), model3.hashCode())
        assertTrue(model1.toString().contains("USD"))

        val (currency, rate) = model1
        assertEquals("USD", currency)
        assertEquals("1 EUR ≈ 1.08 USD", rate)
    }
}
