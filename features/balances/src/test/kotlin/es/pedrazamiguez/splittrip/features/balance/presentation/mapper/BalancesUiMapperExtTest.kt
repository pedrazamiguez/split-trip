package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.GrammaticalGenderEnum
import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BalancesUiMapperExt")
class BalancesUiMapperExtTest {

    private lateinit var userUiMapper: UserUiMapper
    private val testLocale = Locale.US

    @BeforeEach
    fun setUp() {
        userUiMapper = mockk()
    }

    @Nested
    inner class MapCurrencyBreakdowns {

        @Test
        fun `maps empty list to empty list`() {
            val result = mapCurrencyBreakdowns(emptyList(), "EUR", testLocale)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `maps same currency with empty equivalent`() {
            val amounts = listOf(CurrencyAmount(currency = "EUR", amountCents = 1500L, equivalentCents = 1500L))
            val result = mapCurrencyBreakdowns(amounts, "EUR", testLocale)

            assertEquals(1, result.size)
            assertEquals("EUR", result[0].currency)
            assertEquals("€15.00", result[0].formattedAmount)
            assertEquals("", result[0].formattedEquivalent)
        }

        @Test
        fun `maps foreign currency with positive equivalent`() {
            val amounts = listOf(CurrencyAmount(currency = "USD", amountCents = 2000L, equivalentCents = 1800L))
            val result = mapCurrencyBreakdowns(amounts, "EUR", testLocale)

            assertEquals(1, result.size)
            assertEquals("USD", result[0].currency)
            assertEquals("$20.00", result[0].formattedAmount)
            assertEquals("€18.00", result[0].formattedEquivalent)
        }

        @Test
        fun `maps foreign currency with non-positive equivalent to empty equivalent`() {
            val amounts = listOf(CurrencyAmount(currency = "USD", amountCents = 2000L, equivalentCents = 0L))
            val result = mapCurrencyBreakdowns(amounts, "EUR", testLocale)

            assertEquals(1, result.size)
            assertEquals("USD", result[0].currency)
            assertEquals("$20.00", result[0].formattedAmount)
            assertEquals("", result[0].formattedEquivalent)
        }
    }

    @Nested
    inner class MapCashBalances {

        @Test
        fun `sorts cash balances by currency alphabetically`() {
            val pocketBalance = GroupPocketBalance(
                currency = "EUR",
                virtualBalance = 5000L,
                cashBalances = mapOf("USD" to 2000L, "EUR" to 3000L, "GBP" to 1000L),
                cashEquivalents = mapOf("USD" to 1800L, "GBP" to 1100L)
            )

            val result = mapCashBalances(pocketBalance, testLocale)

            assertEquals(3, result.size)
            assertEquals("EUR", result[0].currency)
            assertEquals("€30.00", result[0].formattedAmount)
            assertEquals("", result[0].formattedEquivalent)

            assertEquals("GBP", result[1].currency)
            assertEquals("£10.00", result[1].formattedAmount)
            assertEquals("€11.00", result[1].formattedEquivalent)

            assertEquals("USD", result[2].currency)
            assertEquals("$20.00", result[2].formattedAmount)
            assertEquals("€18.00", result[2].formattedEquivalent)
        }

        @Test
        fun `maps cash balance with missing or zero equivalent to empty string`() {
            val pocketBalance = GroupPocketBalance(
                currency = "EUR",
                cashBalances = mapOf("JPY" to 50000L),
                cashEquivalents = emptyMap()
            )

            val result = mapCashBalances(pocketBalance, testLocale)

            assertEquals(1, result.size)
            assertEquals("JPY", result[0].currency)
            assertEquals("", result[0].formattedEquivalent)
        }
    }

    @Nested
    inner class FormatIfPos {

        @Test
        fun `returns formatted currency when amount is positive`() {
            val result = formatIfPos(2500L, "EUR", testLocale)
            assertEquals("€25.00", result)
        }

        @Test
        fun `returns null when amount is zero`() {
            val result = formatIfPos(0L, "EUR", testLocale)
            assertNull(result)
        }

        @Test
        fun `returns null when amount is negative`() {
            val result = formatIfPos(-500L, "EUR", testLocale)
            assertNull(result)
        }
    }

    @Nested
    inner class ResolveMemberDisplay {

        private val userProfiles = mapOf(
            "user-1" to User(userId = "user-1", email = "test1@test.com", displayName = "Alice"),
            "user-2" to User(userId = "user-2", email = "test2@test.com", displayName = "Bob")
        )

        @Test
        fun `returns Active display when user is in groupMemberIds`() {
            every {
                userUiMapper.mapToDisplayName(
                    user = userProfiles["user-1"],
                    fallbackUserId = "user-1",
                    currentUserId = "user-1",
                    selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
                )
            } returns "You"

            val result = resolveMemberDisplay(
                userId = "user-1",
                groupMemberIds = listOf("user-1", "user-2"),
                memberProfiles = userProfiles,
                currentUserId = "user-1",
                userUiMapper = userUiMapper
            )

            assertTrue(result is MemberDisplay.Active)
            assertEquals("user-1", result.userId)
            assertEquals("You", result.displayName)
        }

        @Test
        fun `returns Former display when user is not in groupMemberIds`() {
            every {
                userUiMapper.mapToDisplayName(
                    user = null,
                    fallbackUserId = "user-old",
                    currentUserId = null,
                    selfIdentificationContext = null
                )
            } returns "Old Member"

            val result = resolveMemberDisplay(
                userId = "user-old",
                groupMemberIds = listOf("user-1", "user-2"),
                memberProfiles = userProfiles,
                currentUserId = null,
                userUiMapper = userUiMapper
            )

            assertTrue(result is MemberDisplay.Former)
            assertEquals("user-old", result.userId)
            assertEquals("Old Member", result.displayName)
        }
    }

    @Nested
    inner class ResolveCreatedByDisplayName {

        private val userProfiles = mapOf(
            "user-1" to User(userId = "user-1", email = "test1@test.com", displayName = "Alice"),
            "user-2" to User(userId = "user-2", email = "test2@test.com", displayName = "Bob")
        )

        @Test
        fun `returns null when createdBy is null or blank`() {
            assertNull(resolveCreatedByDisplayName(null, "user-1", userProfiles, "user-1", userUiMapper))
            assertNull(resolveCreatedByDisplayName("", "user-1", userProfiles, "user-1", userUiMapper))
            assertNull(resolveCreatedByDisplayName("   ", "user-1", userProfiles, "user-1", userUiMapper))
        }

        @Test
        fun `returns null when createdBy equals targetUserId`() {
            assertNull(resolveCreatedByDisplayName("user-1", "user-1", userProfiles, "user-1", userUiMapper))
        }

        @Test
        fun `returns prepositional pronoun when createdBy equals currentUserId`() {
            every {
                userUiMapper.mapToSelfIdentification(
                    SelfIdentificationContextEnum.PREPOSITIONAL,
                    GrammaticalGenderEnum.FEMININE
                )
            } returns "you"

            val result = resolveCreatedByDisplayName("user-1", "user-2", userProfiles, "user-1", userUiMapper)

            assertEquals("you", result)
        }

        @Test
        fun `returns member displayName when createdBy is another user and profile exists`() {
            val result = resolveCreatedByDisplayName("user-2", "user-1", userProfiles, "user-1", userUiMapper)

            assertEquals("Bob", result)
        }

        @Test
        fun `returns null when createdBy is another user and profile is not found`() {
            val result = resolveCreatedByDisplayName("user-unknown", "user-1", userProfiles, "user-1", userUiMapper)

            assertNull(result)
        }
    }
}
