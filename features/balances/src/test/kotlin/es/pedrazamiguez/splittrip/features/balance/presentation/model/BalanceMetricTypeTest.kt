package es.pedrazamiguez.splittrip.features.balance.presentation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BalanceMetricTypeTest {

    @Test
    fun `all BalanceMetricType entries have non-zero string resources`() {
        BalanceMetricType.entries.forEach { metricType ->
            assertNotEquals(0, metricType.titleRes, "titleRes should be non-zero for $metricType")
            assertNotEquals(0, metricType.descriptionRes, "descriptionRes should be non-zero for $metricType")
        }
    }

    @Test
    fun `BalanceMetricType contains expected 8 metric variants`() {
        assertEquals(8, BalanceMetricType.entries.size)
        val names = BalanceMetricType.entries.map { it.name }
        assertTrue(names.contains("REMAINING"))
        assertTrue(names.contains("AVAILABLE"))
        assertTrue(names.contains("SCHEDULED"))
        assertTrue(names.contains("REFUNDABLE"))
        assertTrue(names.contains("TOTAL_CONTRIBUTED"))
        assertTrue(names.contains("TOTAL_SPENT"))
        assertTrue(names.contains("CASH_IN_HAND"))
        assertTrue(names.contains("EXTRAS"))
    }
}
