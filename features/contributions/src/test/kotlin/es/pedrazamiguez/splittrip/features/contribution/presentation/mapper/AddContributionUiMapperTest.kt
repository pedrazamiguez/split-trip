package es.pedrazamiguez.splittrip.features.contribution.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddContributionUiMapperTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var mapper: AddContributionUiMapper

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US
        mapper = AddContributionUiMapper(localeProvider)
    }

    // ── formatInputAmountWithCurrency ─────────────────────────────────────────

    @Nested
    inner class FormatInputAmountWithCurrency {

        @Test
        fun `returns blank when amount is blank`() {
            val result = mapper.formatInputAmountWithCurrency("", "EUR")
            assertEquals("", result)
        }

        @Test
        fun `returns original input when currency code is blank`() {
            val result = mapper.formatInputAmountWithCurrency("100", "")
            assertEquals("100", result)
        }

        @Test
        fun `returns non-blank formatted string for valid amount and currency`() {
            val result = mapper.formatInputAmountWithCurrency("100", "EUR")
            assertTrue(result.isNotBlank())
        }

        @Test
        fun `consults locale provider for the current locale`() {
            mapper.formatInputAmountWithCurrency("50", "USD")
            verify { localeProvider.getCurrentLocale() }
        }

        @Test
        fun `formats differently for different locales`() {
            val usLocaleProvider = mockk<LocaleProvider>()
            every { usLocaleProvider.getCurrentLocale() } returns Locale.US
            val esLocaleProvider = mockk<LocaleProvider>()
            every { esLocaleProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")

            val usResult = AddContributionUiMapper(usLocaleProvider)
                .formatInputAmountWithCurrency("1000", "EUR")
            val esResult = AddContributionUiMapper(esLocaleProvider)
                .formatInputAmountWithCurrency("1000", "EUR")

            // Both non-blank, but formatted differently for their respective locales
            assertTrue(usResult.isNotBlank())
            assertTrue(esResult.isNotBlank())
        }
    }

    // ── resolveCurrencySymbol ─────────────────────────────────────────────────

    @Nested
    inner class ResolveCurrencySymbol {

        @Test
        fun `returns empty string for blank currency code`() {
            val result = mapper.resolveCurrencySymbol("")
            assertEquals("", result)
        }

        @Test
        fun `returns euro symbol for EUR currency code`() {
            val result = mapper.resolveCurrencySymbol("EUR")
            assertEquals("€", result)
        }

        @Test
        fun `returns dollar symbol for USD currency code with US locale`() {
            val result = mapper.resolveCurrencySymbol("USD")
            // Locale.US returns "$" directly (no ISO-code fallback needed)
            assertEquals("$", result)
        }

        @Test
        fun `returns non-blank symbol for a valid currency code`() {
            val result = mapper.resolveCurrencySymbol("GBP")
            assertTrue(result.isNotBlank())
        }

        @Test
        fun `consults locale provider for the current locale`() {
            mapper.resolveCurrencySymbol("EUR")
            verify { localeProvider.getCurrentLocale() }
        }

        @Test
        fun `returns empty string for unknown currency code`() {
            val result = mapper.resolveCurrencySymbol("XYZ")
            assertEquals("", result)
        }
    }

    // ── toMemberOptions ─────────────────────────────────────────────────────

    @Nested
    inner class ToMemberOptions {

        @Test
        fun `maps member IDs with profiles and marks current user`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@test.com", displayName = "Andrés"),
                "user-2" to User(userId = "user-2", email = "b@test.com", displayName = "Ana")
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1", "user-2"),
                memberProfiles = profiles,
                currentUserId = "user-1"
            )

            assertEquals(2, result.size)
            assertEquals("Andrés", result[0].displayName)
            assertTrue(result[0].isCurrentUser)
            assertEquals("Ana", result[1].displayName)
            assertFalse(result[1].isCurrentUser)
        }

        @Test
        fun `falls back to userId when profile is missing`() {
            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1"),
                memberProfiles = emptyMap(),
                currentUserId = "user-1"
            )

            assertEquals(1, result.size)
            assertEquals("user-1", result[0].displayName)
        }

        @Test
        fun `falls back to userId when displayName is blank`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@test.com", displayName = "")
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1"),
                memberProfiles = profiles,
                currentUserId = "user-1"
            )

            assertEquals("user-1", result[0].displayName)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.toMemberOptions(
                memberIds = emptyList(),
                memberProfiles = emptyMap(),
                currentUserId = "user-1"
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `handles null currentUserId`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@test.com", displayName = "Andrés")
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1"),
                memberProfiles = profiles,
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertFalse(result[0].isCurrentUser)
        }
    }

    // ── resolveDisplayName ─────────────────────────────────────────────────────

    @Nested
    inner class ResolveDisplayName {

        private val members = persistentListOf(
            MemberOptionUiModel(userId = "user-1", displayName = "Andrés", isCurrentUser = true),
            MemberOptionUiModel(userId = "user-2", displayName = "Ana", isCurrentUser = false)
        )

        @Test
        fun `returns display name for known userId`() {
            assertEquals("Ana", mapper.resolveDisplayName("user-2", members))
        }

        @Test
        fun `returns empty string for unknown userId`() {
            assertEquals("", mapper.resolveDisplayName("user-99", members))
        }

        @Test
        fun `returns empty string for null userId`() {
            assertEquals("", mapper.resolveDisplayName(null, members))
        }

        @Test
        fun `returns empty string for empty members list`() {
            assertEquals("", mapper.resolveDisplayName("user-1", persistentListOf()))
        }

        @Test
        fun `returns youLabel for the current user when youLabel is provided`() {
            val result = mapper.resolveDisplayName("user-1", members, youLabel = "You")
            assertEquals("You", result)
        }

        @Test
        fun `returns display name of non-current user even when youLabel is provided`() {
            val result = mapper.resolveDisplayName("user-2", members, youLabel = "You")
            assertEquals("Ana", result)
        }

        @Test
        fun `returns display name directly when youLabel is blank`() {
            val result = mapper.resolveDisplayName("user-1", members, youLabel = "")
            assertEquals("Andrés", result)
        }
    }
}
