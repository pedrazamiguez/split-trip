package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.R
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettlementsUiMapperTest {

    private lateinit var mapper: SettlementsUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(R.string.balances_member_you) } returns "You"
        every {
            resourceProvider.getString(DesignR.string.user_pending_fallback)
        } returns "Pending member"
        every { resourceProvider.getString(DesignR.string.settlement_pocket_type_virtual) } returns "Virtual pocket"
        every { resourceProvider.getString(DesignR.string.settlement_pocket_type_cash) } returns "Cash"
        every { resourceProvider.getString(DesignR.string.settlement_pocket_type_net) } returns "Net balance"
        every { resourceProvider.getString(DesignR.string.settlement_status_pending) } returns "Pending"
        every { resourceProvider.getString(DesignR.string.settlement_status_awaiting_confirmation) } returns
            "Awaiting confirmation"
        every { resourceProvider.getString(DesignR.string.settlement_status_confirmed) } returns "Confirmed"
        every { resourceProvider.getString(DesignR.string.settlement_status_disputed) } returns "Disputed"
        every { resourceProvider.getString(DesignR.string.self_identification_nominative) } returns "You"
        every { resourceProvider.getString(DesignR.string.self_identification_beneficiary) } returns "For you"
        every { resourceProvider.getString(DesignR.string.self_identification_agent) } returns "By you"
        every { resourceProvider.getString(DesignR.string.self_identification_recipient) } returns "To you"
        mapper = SettlementsUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
    }

    @Test
    fun `mapSettlements with empty input returns empty list`() {
        val result = mapper.mapSettlements(
            settlements = emptyList(),
            currency = "EUR",
            currentUserId = "user-1",
            memberProfiles = emptyMap()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapSettlements maps and formats amounts correctly`() {
        val settlements = listOf(
            Settlement(fromUserId = "user-2", toUserId = "user-3", amount = 1550L)
        )
        val profiles = listOf(
            User(userId = "user-2", displayName = "Bob", email = "bob@test.com"),
            User(userId = "user-3", displayName = "Charlie", email = "charlie@test.com")
        ).associateBy { it.userId }

        val result = mapper.mapSettlements(
            settlements = settlements,
            currency = "EUR",
            currentUserId = "user-1",
            memberProfiles = profiles
        )

        assertEquals(1, result.size)
        val uiModel = result[0]
        assertEquals("user-2", uiModel.debtorId)
        assertEquals("user-3", uiModel.creditorId)
        assertEquals("Bob", uiModel.debtorName)
        assertEquals("Charlie", uiModel.creditorName)
        assertEquals("€15.50", uiModel.formattedAmount)
        assertFalse(uiModel.isCurrentUserDebtor)
        assertFalse(uiModel.isCurrentUserCreditor)
        assertEquals(SettlementPocketType.NET, uiModel.pocketType)
        assertEquals("EUR", uiModel.currencyCode)
    }

    @Test
    fun `mapSettlements resolves You for current user`() {
        val settlements = listOf(
            Settlement(fromUserId = "user-1", toUserId = "user-2", amount = 1000L),
            Settlement(fromUserId = "user-3", toUserId = "user-1", amount = 500L)
        )
        val profiles = listOf(
            User(userId = "user-1", displayName = "Alice", email = "alice@test.com"),
            User(userId = "user-2", displayName = "Bob", email = "bob@test.com"),
            User(userId = "user-3", displayName = "Charlie", email = "charlie@test.com")
        ).associateBy { it.userId }

        val result = mapper.mapSettlements(
            settlements = settlements,
            currency = "USD",
            currentUserId = "user-1",
            memberProfiles = profiles
        )

        assertEquals(2, result.size)

        val aliceToBob = result.first { it.debtorId == "user-1" }
        assertEquals("You", aliceToBob.debtorName)
        assertEquals("Bob", aliceToBob.creditorName)
        assertTrue(aliceToBob.isCurrentUserDebtor)
        assertFalse(aliceToBob.isCurrentUserCreditor)

        val charlieToAlice = result.first { it.creditorId == "user-1" }
        assertEquals("Charlie", charlieToAlice.debtorName)
        assertEquals("You", charlieToAlice.creditorName)
        assertFalse(charlieToAlice.isCurrentUserDebtor)
        assertTrue(charlieToAlice.isCurrentUserCreditor)
    }

    @Test
    fun `mapSettlements sorts user involved settlements first and then alphabetically`() {
        // Settlements:
        // 1. Bob ("user-2") to Charlie ("user-3") -> User not involved
        // 2. Alice ("user-1") to Dave ("user-4") -> User involved
        // 3. Charlie ("user-3") to Dave ("user-4") -> User not involved
        val settlements = listOf(
            Settlement(fromUserId = "user-2", toUserId = "user-3", amount = 100L),
            Settlement(fromUserId = "user-1", toUserId = "user-4", amount = 200L),
            Settlement(fromUserId = "user-3", toUserId = "user-4", amount = 300L)
        )
        val profiles = listOf(
            User(userId = "user-1", displayName = "Alice", email = "alice@test.com"),
            User(userId = "user-2", displayName = "Bob", email = "bob@test.com"),
            User(userId = "user-3", displayName = "Charlie", email = "charlie@test.com"),
            User(userId = "user-4", displayName = "Dave", email = "dave@test.com")
        ).associateBy { it.userId }

        val result = mapper.mapSettlements(
            settlements = settlements,
            currency = "EUR",
            currentUserId = "user-1",
            memberProfiles = profiles
        )

        assertEquals(3, result.size)
        // First should be user involved (Alice/You to Dave)
        assertEquals("user-1", result[0].debtorId)

        // Next should be sorted alphabetically by debtorName: Bob (Bob) vs Charlie (Charlie) -> Bob first, Charlie second
        assertEquals("user-2", result[1].debtorId)
        assertEquals("user-3", result[2].debtorId)

        // All settlements with default NET pocket type fall back to group currency
        result.forEach { uiModel ->
            assertEquals(SettlementPocketType.NET, uiModel.pocketType)
            assertEquals("EUR", uiModel.currencyCode)
        }
    }

    @Test
    fun `mapSettlements sets pocketType from settlement sourcePocket POCKET`() {
        val settlements = listOf(
            Settlement(
                fromUserId = "user-2",
                toUserId = "user-1",
                amount = 500L,
                sourcePocket = SettlementPocketType.POCKET,
                currency = "EUR"
            )
        )
        val profiles = mapOf(
            "user-2" to User(userId = "user-2", displayName = "Bob", email = "bob@test.com")
        )
        val result = mapper.mapSettlements(settlements, "EUR", "user-1", profiles)
        assertEquals(SettlementPocketType.POCKET, result[0].pocketType)
    }

    @Test
    fun `mapSettlements sets pocketType from settlement sourcePocket CASH`() {
        val settlements = listOf(
            Settlement(
                fromUserId = "user-1",
                toUserId = "user-2",
                amount = 300L,
                sourcePocket = SettlementPocketType.CASH,
                currency = "THB"
            )
        )
        val profiles = mapOf(
            "user-2" to User(userId = "user-2", displayName = "Bob", email = "bob@test.com")
        )
        val result = mapper.mapSettlements(settlements, "EUR", "user-1", profiles)
        assertEquals(SettlementPocketType.CASH, result[0].pocketType)
    }

    @Test
    fun `mapSettlements uses settlement currency for formattedAmount when non-empty`() {
        val settlements = listOf(
            Settlement(
                fromUserId = "user-2",
                toUserId = "user-1",
                amount = 5000L,
                currency = "THB",
                sourcePocket = SettlementPocketType.CASH
            )
        )
        val profiles = mapOf(
            "user-2" to User(userId = "user-2", displayName = "Bob", email = "bob@test.com")
        )
        val result = mapper.mapSettlements(settlements, "EUR", "user-1", profiles)
        assertEquals("THB", result[0].currencyCode)
    }

    @Test
    fun `mapSettlements falls back to group currency when settlement currency is empty`() {
        val settlements = listOf(
            Settlement(fromUserId = "user-2", toUserId = "user-1", amount = 100L)
        )
        val profiles = mapOf(
            "user-2" to User(userId = "user-2", displayName = "Bob", email = "bob@test.com")
        )
        val result = mapper.mapSettlements(settlements, "GBP", "user-1", profiles)
        assertEquals("GBP", result[0].currencyCode)
    }

    @Test
    fun `mapSettlements maps pocketTypeLabel for POCKET source`() {
        val settlement = Settlement(
            fromUserId = "user-2",
            toUserId = "user-1",
            amount = 500L,
            sourcePocket = SettlementPocketType.POCKET,
            currency = "EUR"
        )
        val result = mapper.mapSettlements(
            listOf(settlement),
            "EUR",
            "user-1",
            mapOf(
                "user-2" to User("user-2", "Bob", "bob@test.com")
            )
        )
        assertEquals("Virtual pocket", result[0].pocketTypeLabel)
    }

    @Test
    fun `mapSettlements maps pocketTypeLabel for CASH source`() {
        val settlement = Settlement(
            fromUserId = "user-2",
            toUserId = "user-1",
            amount = 300L,
            sourcePocket = SettlementPocketType.CASH,
            currency = "THB"
        )
        val result = mapper.mapSettlements(
            listOf(settlement),
            "EUR",
            "user-1",
            mapOf(
                "user-2" to User("user-2", "Bob", "bob@test.com")
            )
        )
        assertEquals("Cash", result[0].pocketTypeLabel)
    }

    @Test
    fun `mapSettlements maps pocketTypeLabel for NET source`() {
        val settlement = Settlement(
            fromUserId = "user-2",
            toUserId = "user-1",
            amount = 100L,
            sourcePocket = SettlementPocketType.NET
        )
        val result = mapper.mapSettlements(
            listOf(settlement),
            "EUR",
            "user-1",
            mapOf(
                "user-2" to User("user-2", "Bob", "bob@test.com")
            )
        )
        assertEquals("Net balance", result[0].pocketTypeLabel)
    }

    @Test
    fun `mapSettlements sets status to SUGGESTED by default`() {
        val settlement = Settlement(fromUserId = "user-2", toUserId = "user-1", amount = 100L)
        val result = mapper.mapSettlements(
            listOf(settlement),
            "EUR",
            "user-1",
            mapOf(
                "user-2" to User("user-2", "Bob", "bob@test.com")
            )
        )
        assertEquals(SettlementStatus.SUGGESTED, result[0].status)
    }

    @Test
    fun `mapSettlements maps statusLabel and statusChipStyle for SUGGESTED status`() {
        val settlement = Settlement(fromUserId = "user-2", toUserId = "user-1", amount = 100L)
        val result = mapper.mapSettlements(
            listOf(settlement),
            "EUR",
            "user-1",
            mapOf(
                "user-2" to User("user-2", "Bob", "bob@test.com")
            )
        )
        assertEquals("Pending", result[0].statusLabel)
        assertEquals(PassportTagStyle.NEUTRAL, result[0].statusChipStyle)
    }
}
