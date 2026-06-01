package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class BalanceTest {

    private val eur = Currency("EUR", "€", "Euro", 2)
    private val usd = Currency("USD", "$", "US Dollar", 2)

    @Test
    fun `equals returns true for same fields`() {
        val balance1 = Balance("user_1", BigDecimal("10.00"), eur)
        val balance2 = Balance("user_1", BigDecimal("10.00"), eur)

        assertEquals(balance1, balance2)
    }

    @Test
    fun `equals returns false for different fields`() {
        val balance1 = Balance("user_1", BigDecimal("10.00"), eur)
        val balance2 = Balance("user_2", BigDecimal("10.00"), eur)
        val balance3 = Balance("user_1", BigDecimal("20.00"), eur)
        val balance4 = Balance("user_1", BigDecimal("10.00"), usd)

        assertNotEquals(balance1, balance2)
        assertNotEquals(balance1, balance3)
        assertNotEquals(balance1, balance4)
    }

    @Test
    fun `hashCode is consistent`() {
        val balance1 = Balance("user_1", BigDecimal("10.00"), eur)
        val balance2 = Balance("user_1", BigDecimal("10.00"), eur)

        assertEquals(balance1.hashCode(), balance2.hashCode())
    }

    @Test
    fun `toString output format is correct`() {
        val balance = Balance("user_1", BigDecimal("10.00"), eur)
        val expected = "Balance(userId=user_1, amount=10.00, " +
            "currency=Currency(code=EUR, symbol=€, defaultName=Euro, decimalDigits=2))"

        assertEquals(expected, balance.toString())
    }
}
