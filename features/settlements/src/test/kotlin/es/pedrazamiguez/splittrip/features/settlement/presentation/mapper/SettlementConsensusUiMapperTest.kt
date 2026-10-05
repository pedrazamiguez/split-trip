package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.settlement.R
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettlementConsensusUiMapperTest {

    private val localeProvider: LocaleProvider = mockk()
    private val resourceProvider: ResourceProvider = mockk()
    private lateinit var mapper: SettlementConsensusUiMapper

    @BeforeEach
    fun setUp() {
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(eq(R.string.your_balance_settlement_you_owe), *anyVararg()) } answers {
            val args = secondArg<Array<Any>>()
            "You owe ${args[0]}"
        }
        every { resourceProvider.getString(eq(R.string.your_balance_settlement_owes_you), *anyVararg()) } answers {
            val args = secondArg<Array<Any>>()
            "${args[0]} owes you"
        }
        every { resourceProvider.getString(R.string.your_balance_settlement_mark_paid) } returns "Mark as paid"
        every { resourceProvider.getString(R.string.your_balance_settlement_confirm_receipt) } returns
            "Confirm receipt"
        every { resourceProvider.getString(R.string.your_balance_settlement_resolve_dispute) } returns
            "Resolve dispute"
        every { resourceProvider.getString(R.string.your_balance_settlement_dispute) } returns "Dispute"
        every { resourceProvider.getString(R.string.your_balance_settlement_status_suggested) } returns "Suggested"
        every { resourceProvider.getString(R.string.your_balance_settlement_status_confirmed_payer) } returns
            "Confirmed by payer"
        every { resourceProvider.getString(R.string.your_balance_settlement_status_disputed) } returns "Disputed"
        every { resourceProvider.getString(R.string.your_balance_settlement_pocket_pocket) } returns "Pocket"
        every { resourceProvider.getString(R.string.your_balance_settlement_pocket_cash) } returns "Cash"
        every { resourceProvider.getString(R.string.your_balance_settlement_pocket_net) } returns "Net"
        every { resourceProvider.getString(R.string.your_balance_settlement_remind) } returns "Remind"
        every { resourceProvider.getString(R.string.your_balance_settlement_reminded) } returns "Reminded"

        mapper = SettlementConsensusUiMapper(localeProvider, resourceProvider)
    }

    private fun createRecord(
        id: String,
        fromUserId: String,
        toUserId: String,
        amount: Long = 2500L,
        status: SettlementStatus = SettlementStatus.SUGGESTED,
        disputeReason: String? = null
    ) = SettlementRecord(
        id = id,
        groupId = "g1",
        settlement = Settlement(
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amount,
            currency = "EUR",
            sourcePocket = SettlementPocketType.NET
        ),
        status = status,
        createdAt = LocalDateTime.now(),
        disputeReason = disputeReason
    )

    @Test
    fun `filters only active settlements involving current user`() {
        val records = listOf(
            createRecord("s1", "user1", "user2", status = SettlementStatus.SUGGESTED),
            createRecord("s2", "user2", "user3", status = SettlementStatus.SUGGESTED),
            createRecord("s3", "user3", "user1", status = SettlementStatus.RESOLVED),
            createRecord("s4", "user3", "user1", status = SettlementStatus.DISPUTED)
        )

        val result = mapper.toConsensusItems(records, "user1", "creator1", emptyMap())

        assertEquals(2, result.size)
        assertEquals(listOf("s4", "s1"), result.map { it.settlementId })
    }

    @Test
    fun `returns empty list when no active settlements involve user`() {
        val records = listOf(
            createRecord("s1", "user2", "user3", status = SettlementStatus.SUGGESTED)
        )

        val result = mapper.toConsensusItems(records, "user1", "creator1", emptyMap())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `payer in SUGGESTED can confirm and dispute for NET`() {
        val record = createRecord("s1", "user1", "user2", status = SettlementStatus.SUGGESTED)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        assertEquals(1, result.size)
        val item = result[0]
        assertTrue(item.isCurrentUserPayer)
        assertTrue(item.canConfirm)
        assertEquals("Mark as paid", item.confirmLabel)
        assertTrue(item.canDispute)
    }

    @Test
    fun `payer in SUGGESTED maps Spanish string resource without truncation`() {
        every { resourceProvider.getString(R.string.your_balance_settlement_mark_paid) } returns "He pagado"
        val record = createRecord("s1", "user1", "user2", status = SettlementStatus.SUGGESTED)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        assertEquals(1, result.size)
        val item = result[0]
        assertTrue(item.isCurrentUserPayer)
        assertTrue(item.canConfirm)
        assertEquals("He pagado", item.confirmLabel)
        assertTrue(item.canDispute)
    }

    @Test
    fun `payer in SUGGESTED cannot confirm or dispute for CASH`() {
        val record = createRecord("s1", "user1", "user2", status = SettlementStatus.SUGGESTED)
            .copy(
                settlement = Settlement(
                    fromUserId = "user1",
                    toUserId = "user2",
                    amount = 2500L,
                    currency = "EUR",
                    sourcePocket = SettlementPocketType.CASH
                )
            )

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        assertEquals(1, result.size)
        val item = result[0]
        assertTrue(item.isCurrentUserPayer)
        assertFalse(item.canConfirm)
        assertEquals("", item.confirmLabel)
        assertFalse(item.canDispute)
    }

    @Test
    fun `payee in SUGGESTED cannot confirm but can dispute`() {
        val record = createRecord("s1", "user2", "user1", status = SettlementStatus.SUGGESTED)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        val item = result[0]
        assertFalse(item.isCurrentUserPayer)
        assertFalse(item.canConfirm)
        assertTrue(item.canDispute)
    }

    @Test
    fun `payee in CONFIRMED_BY_PAYER can confirm and dispute`() {
        val record = createRecord("s1", "user2", "user1", status = SettlementStatus.CONFIRMED_BY_PAYER)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        val item = result[0]
        assertTrue(item.canConfirm)
        assertEquals("Confirm receipt", item.confirmLabel)
        assertTrue(item.canDispute)
    }

    @Test
    fun `payer in CONFIRMED_BY_PAYER cannot confirm or dispute`() {
        val record = createRecord("s1", "user1", "user2", status = SettlementStatus.CONFIRMED_BY_PAYER)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        val item = result[0]
        assertFalse(item.canConfirm)
        assertFalse(item.canDispute)
    }

    @Test
    fun `payee in DISPUTED can resolve`() {
        val record =
            createRecord("s1", "user2", "user1", status = SettlementStatus.DISPUTED, disputeReason = "Wrong amount")

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator2", emptyMap())

        val item = result[0]
        assertTrue(item.canConfirm)
        assertEquals("Resolve dispute", item.confirmLabel)
        assertFalse(item.canDispute)
        assertEquals("Wrong amount", item.disputeReason)
    }

    @Test
    fun `group creator in DISPUTED can resolve`() {
        val record = createRecord("s1", "user2", "user3", status = SettlementStatus.DISPUTED)

        val result = mapper.toConsensusItems(listOf(record), "user2", "user2", emptyMap())

        val item = result[0]
        assertTrue(item.canConfirm)
        assertEquals("Resolve dispute", item.confirmLabel)
    }

    @Test
    fun `payer in DISPUTED without creator role cannot act`() {
        val record = createRecord("s1", "user1", "user2", status = SettlementStatus.DISPUTED)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        val item = result[0]
        assertFalse(item.canConfirm)
        assertFalse(item.canDispute)
    }

    @Test
    fun `sorts DISPUTED first then CONFIRMED_BY_PAYER then SUGGESTED`() {
        val records = listOf(
            createRecord("s1", "user1", "user2", status = SettlementStatus.SUGGESTED),
            createRecord("s2", "user1", "user2", status = SettlementStatus.DISPUTED),
            createRecord("s3", "user1", "user2", status = SettlementStatus.CONFIRMED_BY_PAYER)
        )

        val result = mapper.toConsensusItems(records, "user1", "creator1", emptyMap())

        assertEquals(listOf("s2", "s3", "s1"), result.map { it.settlementId })
    }

    @Test
    fun `formats amount using formatCurrencyAmount`() {
        val record = createRecord("s1", "user1", "user2", amount = 2500L)

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        assertEquals("€25.00", result[0].formattedAmount)
    }

    @Test
    fun `resolves counterparty name from member profiles`() {
        val record = createRecord("s1", "user1", "user2")
        val profiles = mapOf("user2" to User(userId = "user2", email = "user2@test.com", displayName = "Alice"))

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", profiles)

        assertEquals("Alice", result[0].counterpartyName)
        assertEquals("You owe Alice", result[0].directionLabel)
    }

    @Test
    fun `falls back to userId when member profile not found`() {
        val record = createRecord("s1", "user1", "user2")

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", emptyMap())

        assertEquals("user2", result[0].counterpartyName)
        assertEquals("You owe user2", result[0].directionLabel)
    }

    @Test
    fun `direction label shows owes you for payee`() {
        val record = createRecord("s1", "user2", "user1")
        val profiles = mapOf("user2" to User(userId = "user2", email = "user2@test.com", displayName = "Bob"))

        val result = mapper.toConsensusItems(listOf(record), "user1", "creator1", profiles)

        assertEquals("Bob owes you", result[0].directionLabel)
    }

    @Test
    fun `toConsensusItems_whenUserIsCreditorAndNotRateLimited_setsCanNudgeTrueAndIsRateLimitedFalse`() {
        val record = createRecord("s1", "debtor1", "creditor1")
        val now = 100_000_000L

        val result = mapper.toConsensusItems(
            settlements = listOf(record),
            currentUserId = "creditor1",
            groupCreatorId = "creator1",
            memberProfiles = emptyMap(),
            nudgeTimestamps = emptyMap(),
            rateLimitHours = 24L,
            currentTimeMillis = now
        )

        val item = result[0]
        assertTrue(item.canNudge)
        assertFalse(item.isNudgeRateLimited)
        assertEquals("Remind", item.nudgeButtonLabel)
    }

    @Test
    fun `toConsensusItems_whenUserIsCreditorAndRateLimited_setsCanNudgeTrueAndIsRateLimitedTrue`() {
        val record = createRecord("s1", "debtor1", "creditor1")
        val now = 100_000_000L
        val lastNudgeTs = now - (12 * 3600 * 1000L) // 12 hours ago (within 24h)

        val result = mapper.toConsensusItems(
            settlements = listOf(record),
            currentUserId = "creditor1",
            groupCreatorId = "creator1",
            memberProfiles = emptyMap(),
            nudgeTimestamps = mapOf("s1" to lastNudgeTs),
            rateLimitHours = 24L,
            currentTimeMillis = now
        )

        val item = result[0]
        assertTrue(item.canNudge)
        assertTrue(item.isNudgeRateLimited)
        assertEquals("Reminded", item.nudgeButtonLabel)
    }

    @Test
    fun `toConsensusItems_whenUserIsDebtor_setsCanNudgeFalse`() {
        val record = createRecord("s1", "debtor1", "creditor1")

        val result = mapper.toConsensusItems(
            settlements = listOf(record),
            currentUserId = "debtor1",
            groupCreatorId = "creator1",
            memberProfiles = emptyMap()
        )

        val item = result[0]
        assertFalse(item.canNudge)
    }

    @Test
    fun `toConsensusItems_whenPocketIsCash_setsCanNudgeFalseAndNoLabel`() {
        val record = createRecord("s1", "debtor1", "creditor1").copy(
            settlement = Settlement(
                fromUserId = "debtor1",
                toUserId = "creditor1",
                amount = 2500L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        )

        val result = mapper.toConsensusItems(
            settlements = listOf(record),
            currentUserId = "creditor1",
            groupCreatorId = "creator1",
            memberProfiles = emptyMap()
        )

        val item = result[0]
        assertFalse(item.canNudge)
        assertTrue(item.isNudgeRateLimited)
        assertEquals("", item.nudgeButtonLabel)
    }
}
