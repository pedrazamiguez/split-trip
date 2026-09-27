package es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupUiMapperImplCurrencyRatesTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: GroupUiMapperImpl

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()
        userUiMapper = mockk(relaxed = true)
        mapper = GroupUiMapperImpl(localeProvider, resourceProvider, userUiMapper)
    }

    @Nested
    inner class MapCurrencyExchangeRates {

        @Test
        fun `mapCurrencyExchangeRates formats available rates using locale number format`() {
            // US locale test
            every { localeProvider.getCurrentLocale() } returns Locale.US
            every {
                resourceProvider.getString(
                    R.string.group_detail_exchange_rate_format,
                    "EUR",
                    "19.82",
                    "ZAR"
                )
            } returns "1 EUR ≈ 19.82 ZAR"

            val usRates = mapOf("ZAR" to BigDecimal("19.82"))
            val usResult = mapper.mapCurrencyExchangeRates("EUR", usRates)

            assertEquals(1, usResult.size)
            assertEquals("ZAR", usResult[0].currency)
            assertEquals("1 EUR ≈ 19.82 ZAR", usResult[0].formattedRate)

            // ES locale test
            every { localeProvider.getCurrentLocale() } returns Locale("es", "ES")
            every {
                resourceProvider.getString(
                    R.string.group_detail_exchange_rate_format,
                    "EUR",
                    "19,82",
                    "ZAR"
                )
            } returns "1 EUR ≈ 19,82 ZAR"

            val esResult = mapper.mapCurrencyExchangeRates("EUR", usRates)

            assertEquals(1, esResult.size)
            assertEquals("ZAR", esResult[0].currency)
            assertEquals("1 EUR ≈ 19,82 ZAR", esResult[0].formattedRate)
        }

        @Test
        fun `mapCurrencyExchangeRates produces null formattedRate when rate is null`() {
            every { localeProvider.getCurrentLocale() } returns Locale.US

            val rates = mapOf<String, BigDecimal?>("USD" to null)
            val result = mapper.mapCurrencyExchangeRates("EUR", rates)

            assertEquals(1, result.size)
            assertEquals(GroupCurrencyRateUiModel(currency = "USD", formattedRate = null), result[0])
            assertNull(result[0].formattedRate)
        }
    }
}
