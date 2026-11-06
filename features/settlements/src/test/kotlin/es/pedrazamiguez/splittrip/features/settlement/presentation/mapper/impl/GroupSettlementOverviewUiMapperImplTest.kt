package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.GroupSettlementOverviewUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.archive.ArchiveWizardStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupSettlementOverviewUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: GroupSettlementOverviewUiMapperImpl

    private val testLocale = Locale.US
    private val currentUserId = "user-1"

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns testLocale
        }
        resourceProvider = mockk(relaxed = true)
        every { resourceProvider.getString(DesignSystemR.string.self_identification_nominative) } returns "You"
        every { resourceProvider.getString(DesignSystemR.string.user_pending_fallback) } returns "Pending member"
        formattingHelper = FormattingHelper(localeProvider)
        userUiMapper = UserUiMapper(resourceProvider)
        mapper = GroupSettlementOverviewUiMapperImpl(formattingHelper, resourceProvider, userUiMapper)
    }

    @Nested
    inner class EmptySettlements {

        @Test
        fun `empty settlements list produces empty groups and areAllSettlementsResolved true`() {
            val result = mapper.toUiState(emptyList(), emptyMap(), currentUserId)

            assertFalse(result.isLoading)
            assertTrue(result.areAllSettlementsResolved)
            assertTrue(result.pendingSettlements.isEmpty())
            assertTrue(result.disputedSettlements.isEmpty())
            assertTrue(result.resolvedSettlements.isEmpty())
        }
    }

    @Nested
    inner class SettlementGrouping {

        @Test
        fun `suggested settlements are grouped as pending`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertEquals(1, result.pendingSettlements.size)
            assertTrue(result.disputedSettlements.isEmpty())
            assertTrue(result.resolvedSettlements.isEmpty())
            assertFalse(result.areAllSettlementsResolved)
        }

        @Test
        fun `confirmed_by_payer settlements are grouped as pending`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.CONFIRMED_BY_PAYER)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertEquals(1, result.pendingSettlements.size)
            assertTrue(result.disputedSettlements.isEmpty())
            assertTrue(result.resolvedSettlements.isEmpty())
            assertFalse(result.areAllSettlementsResolved)
        }

        @Test
        fun `disputed settlements are grouped separately`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.DISPUTED)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertTrue(result.pendingSettlements.isEmpty())
            assertEquals(1, result.disputedSettlements.size)
            assertTrue(result.resolvedSettlements.isEmpty())
            assertFalse(result.areAllSettlementsResolved)
        }

        @Test
        fun `resolved settlements are grouped separately`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.RESOLVED)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertTrue(result.pendingSettlements.isEmpty())
            assertTrue(result.disputedSettlements.isEmpty())
            assertEquals(1, result.resolvedSettlements.size)
            assertTrue(result.areAllSettlementsResolved)
        }

        @Test
        fun `all resolved returns areAllSettlementsResolved true`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.RESOLVED),
                createRecord("s-2", SettlementStatus.RESOLVED)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertTrue(result.areAllSettlementsResolved)
        }

        @Test
        fun `mixed status returns areAllSettlementsResolved false`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED),
                createRecord("s-2", SettlementStatus.RESOLVED)
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            assertFalse(result.areAllSettlementsResolved)
        }
    }

    @Nested
    inner class SettlementRowMapping {

        @Test
        fun `maps debtor and creditor names from member profiles`() {
            every {
                resourceProvider.getString(R.string.settlement_overview_direction_other_owes, "Alice", "Bob")
            } returns "Alice owes Bob"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@b.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "c@d.com", displayName = "Bob")
            )
            val result = mapper.toUiState(settlements, profiles, "user-3")

            val row = result.pendingSettlements.first()
            assertEquals("Alice", row.debtorName)
            assertEquals("Bob", row.creditorName)
            assertEquals("Alice owes Bob", row.directionTitle)
        }

        @Test
        fun `uses You resource for current user as debtor or creditor`() {
            every {
                resourceProvider.getString(DesignSystemR.string.balance_you)
            } returns "You"
            every {
                resourceProvider.getString(R.string.settlement_overview_direction_you_owe, "Bob")
            } returns "You owe Bob"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@b.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "c@d.com", displayName = "Bob")
            )
            val result = mapper.toUiState(settlements, profiles, "user-1")

            val row = result.pendingSettlements.first()
            assertEquals("You", row.debtorName)
            assertEquals("Bob", row.creditorName)
            assertEquals("You owe Bob", row.directionTitle)
        }

        @Test
        fun `uses fallback for unknown member profiles`() {
            every {
                resourceProvider.getString(DesignSystemR.string.user_pending_fallback)
            } returns "Pending member"

            val settlements = listOf(
                createRecord(
                    "s-1",
                    SettlementStatus.SUGGESTED,
                    fromUser = "pending_member_1",
                    toUser = "pending_member_2"
                )
            )
            val result = mapper.toUiState(settlements, emptyMap(), currentUserId)

            val row = result.pendingSettlements.first()
            assertEquals("Pending member", row.debtorName)
            assertEquals("Pending member", row.creditorName)
        }

        @Test
        fun `marks current user as debtor when fromUserId matches`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-1")

            val row = result.pendingSettlements.first()
            assertTrue(row.isCurrentUserDebtor)
            assertFalse(row.isCurrentUserCreditor)
        }

        @Test
        fun `marks current user as creditor when toUserId matches`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-2", toUser = "user-1")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-1")

            val row = result.pendingSettlements.first()
            assertFalse(row.isCurrentUserDebtor)
            assertTrue(row.isCurrentUserCreditor)
        }

        @Test
        fun `payer can confirm in SUGGESTED state`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-1")

            val row = result.pendingSettlements.first()
            assertTrue(row.canCurrentUserConfirm)
            assertTrue(row.canCurrentUserDispute)
        }

        @Test
        fun `payee can confirm in CONFIRMED_BY_PAYER state`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.CONFIRMED_BY_PAYER, fromUser = "user-1", toUser = "user-2")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-2")

            val row = result.pendingSettlements.first()
            assertTrue(row.canCurrentUserConfirm)
            assertTrue(row.canCurrentUserDispute)
        }

        @Test
        fun `non-involved user cannot confirm or dispute`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-3")

            val row = result.pendingSettlements.first()
            assertFalse(row.canCurrentUserConfirm)
            assertFalse(row.canCurrentUserDispute)
        }

        @Test
        fun `resolved settlement has no confirm or dispute actions`() {
            val settlements = listOf(
                createRecord("s-1", SettlementStatus.RESOLVED, fromUser = "user-1", toUser = "user-2")
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-1")

            val row = result.resolvedSettlements.first()
            assertFalse(row.canCurrentUserConfirm)
            assertFalse(row.canCurrentUserDispute)
        }

        @Test
        fun `disputed settlement has no confirm action but shows dispute reason`() {
            val settlements = listOf(
                createRecord(
                    "s-1",
                    SettlementStatus.DISPUTED,
                    fromUser = "user-1",
                    toUser = "user-2",
                    disputedBy = "user-1",
                    disputeReason = "Amount incorrect"
                )
            )
            val result = mapper.toUiState(settlements, emptyMap(), "user-1")

            val row = result.disputedSettlements.first()
            assertFalse(row.canCurrentUserConfirm)
            assertFalse(row.canCurrentUserDispute)
            assertEquals("Amount incorrect", row.disputeReason)
            assertTrue(row.disputedByCurrentUser)
        }

        @Test
        fun `status labels are resolved from string resources`() {
            every { resourceProvider.getString(R.string.settlement_overview_status_suggested) } returns "Pending"
            every { resourceProvider.getString(R.string.settlement_overview_status_confirmed_by_payer) } returns
                "Awaiting"
            every { resourceProvider.getString(R.string.settlement_overview_status_disputed) } returns "Disputed"
            every { resourceProvider.getString(R.string.settlement_overview_status_resolved) } returns "Resolved"

            val suggested = createRecord("s-1", SettlementStatus.SUGGESTED)
            val confirmed = createRecord("s-2", SettlementStatus.CONFIRMED_BY_PAYER)
            val disputed = createRecord("s-3", SettlementStatus.DISPUTED)
            val resolved = createRecord("s-4", SettlementStatus.RESOLVED)

            val result = mapper.toUiState(listOf(suggested, confirmed, disputed, resolved), emptyMap(), currentUserId)

            assertEquals("Pending", result.pendingSettlements[0].statusLabel)
            assertEquals("Awaiting", result.pendingSettlements[1].statusLabel)
            assertEquals("Disputed", result.disputedSettlements[0].statusLabel)
            assertEquals("Resolved", result.resolvedSettlements[0].statusLabel)
        }

        @Test
        fun `status chip styles are mapped correctly`() {
            val suggested = createRecord("s-1", SettlementStatus.SUGGESTED)
            val confirmed = createRecord("s-2", SettlementStatus.CONFIRMED_BY_PAYER)
            val disputed = createRecord("s-3", SettlementStatus.DISPUTED)
            val resolved = createRecord("s-4", SettlementStatus.RESOLVED)

            val result = mapper.toUiState(listOf(suggested, confirmed, disputed, resolved), emptyMap(), currentUserId)

            assertEquals(PassportTagStyle.NEUTRAL, result.pendingSettlements[0].statusChipStyle)
            assertEquals(PassportTagStyle.WARNING, result.pendingSettlements[1].statusChipStyle)
            assertEquals(PassportTagStyle.ERROR, result.disputedSettlements[0].statusChipStyle)
            assertEquals(PassportTagStyle.SUCCESS, result.resolvedSettlements[0].statusChipStyle)
        }

        @Test
        fun `directionTitle is owes_you when current user is creditor`() {
            every {
                resourceProvider.getString(R.string.settlement_overview_direction_owes_you, "Antonio")
            } returns "Antonio owes you"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-2", toUser = "user-1")
            )
            val profiles = mapOf(
                "user-2" to User(userId = "user-2", email = "a@b.com", displayName = "Antonio")
            )
            val result = mapper.toUiState(settlements, profiles, "user-1")

            val row = result.pendingSettlements.first()
            assertEquals("Antonio owes you", row.directionTitle)
        }

        @Test
        fun `directionTitle is you_owe when current user is debtor`() {
            every {
                resourceProvider.getString(R.string.settlement_overview_direction_you_owe, "Antonio")
            } returns "You owe Antonio"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            )
            val profiles = mapOf(
                "user-2" to User(userId = "user-2", email = "a@b.com", displayName = "Antonio")
            )
            val result = mapper.toUiState(settlements, profiles, "user-1")

            val row = result.pendingSettlements.first()
            assertEquals("You owe Antonio", row.directionTitle)
        }

        @Test
        fun `directionTitle is other_owes when current user is uninvolved`() {
            every {
                resourceProvider.getString(R.string.settlement_overview_direction_other_owes, "Antonio", "Bernardo")
            } returns "Antonio owes Bernardo"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "user-2", toUser = "user-3")
            )
            val profiles = mapOf(
                "user-2" to User(userId = "user-2", email = "a@b.com", displayName = "Antonio"),
                "user-3" to User(userId = "user-3", email = "c@d.com", displayName = "Bernardo")
            )
            val result = mapper.toUiState(settlements, profiles, "user-1")

            val row = result.pendingSettlements.first()
            assertEquals("Antonio owes Bernardo", row.directionTitle)
        }

        @Test
        fun `directionTitle uses fallback for unknown member in other_owes`() {
            every {
                resourceProvider.getString(DesignSystemR.string.user_pending_fallback)
            } returns "Pending member"
            every {
                resourceProvider.getString(
                    R.string.settlement_overview_direction_other_owes,
                    "Pending member",
                    "Bernardo"
                )
            } returns "Pending member owes Bernardo"

            val settlements = listOf(
                createRecord("s-1", SettlementStatus.SUGGESTED, fromUser = "pending_member", toUser = "user-3")
            )
            val profiles = mapOf(
                "user-3" to User(userId = "user-3", email = "c@d.com", displayName = "Bernardo")
            )
            val result = mapper.toUiState(settlements, profiles, "user-1")

            val row = result.pendingSettlements.first()
            assertEquals("Pending member owes Bernardo", row.directionTitle)
        }
    }

    private fun createRecord(
        id: String,
        status: SettlementStatus,
        fromUser: String = "user-1",
        toUser: String = "user-2",
        amount: Long = 10000L,
        disputedBy: String? = null,
        disputeReason: String? = null
    ): SettlementRecord = SettlementRecord(
        id = id,
        groupId = "group-1",
        settlement = Settlement(
            fromUserId = fromUser,
            toUserId = toUser,
            amount = amount,
            currency = "EUR",
            sourcePocket = SettlementPocketType.NET
        ),
        status = status,
        createdAt = LocalDateTime.now(),
        disputedBy = disputedBy,
        disputeReason = disputeReason
    )

    private fun GroupSettlementOverviewUiMapper.toUiState(
        settlements: List<SettlementRecord>,
        memberProfiles: Map<String, User>,
        currentUserId: String
    ): GroupSettlementOverviewUiState {
        return toUiState(
            settlements = settlements,
            memberProfiles = memberProfiles,
            currentUserId = currentUserId,
            groupCreatorId = "user-1",
            groupName = "Test Group"
        )
    }

    @Nested
    inner class ArchiveWizardMapping {

        @Test
        fun `maps creator status and group name correctly`() {
            val resultCreator = mapper.toUiState(
                settlements = emptyList(),
                memberProfiles = emptyMap(),
                currentUserId = "creator-id",
                groupCreatorId = "creator-id",
                groupName = "Creator Group"
            )
            assertTrue(resultCreator.isUserCreator)
            assertEquals("Creator Group", resultCreator.groupName)

            val resultMember = mapper.toUiState(
                settlements = emptyList(),
                memberProfiles = emptyMap(),
                currentUserId = "member-id",
                groupCreatorId = "creator-id",
                groupName = "Member Group"
            )
            assertFalse(resultMember.isUserCreator)
            assertEquals("Member Group", resultMember.groupName)
        }

        @Test
        fun `maps activeSteps streamlined when all resolved`() {
            val suggested = createRecord("1", SettlementStatus.RESOLVED)
            val result = mapper.toUiState(
                settlements = listOf(suggested),
                memberProfiles = emptyMap(),
                currentUserId = "user-1",
                groupCreatorId = "user-1",
                groupName = "Test Group"
            )
            assertTrue(result.areAllSettlementsResolved)
            assertEquals(
                listOf(ArchiveWizardStep.SETTLEMENT_SUMMARY, ArchiveWizardStep.CONFIRMATION),
                result.activeSteps
            )
        }

        @Test
        fun `maps activeSteps streamlined when settlements is empty`() {
            val result = mapper.toUiState(
                settlements = emptyList(),
                memberProfiles = emptyMap(),
                currentUserId = "user-1",
                groupCreatorId = "user-1",
                groupName = "Test Group"
            )
            assertTrue(result.areAllSettlementsResolved)
            assertEquals(
                listOf(ArchiveWizardStep.SETTLEMENT_SUMMARY, ArchiveWizardStep.CONFIRMATION),
                result.activeSteps
            )
        }

        @Test
        fun `maps activeSteps multi-step when unresolved exists`() {
            val suggested = createRecord("1", SettlementStatus.SUGGESTED)
            val result = mapper.toUiState(
                settlements = listOf(suggested),
                memberProfiles = emptyMap(),
                currentUserId = "user-1",
                groupCreatorId = "user-1",
                groupName = "Test Group"
            )
            assertFalse(result.areAllSettlementsResolved)
            assertEquals(
                listOf(
                    ArchiveWizardStep.SETTLEMENT_SUMMARY,
                    ArchiveWizardStep.CONFIRMATION
                ),
                result.activeSteps
            )
        }

        @Test
        fun `counts sections correctly`() {
            val pendingAction = createRecord("1", SettlementStatus.SUGGESTED, fromUser = "user-1", toUser = "user-2")
            val pendingWaiting = createRecord("2", SettlementStatus.SUGGESTED, fromUser = "user-2", toUser = "user-1")
            val disputed = createRecord("3", SettlementStatus.DISPUTED)

            val result = mapper.toUiState(
                settlements = listOf(pendingAction, pendingWaiting, disputed),
                memberProfiles = emptyMap(),
                currentUserId = "user-1",
                groupCreatorId = "user-1",
                groupName = "Test Group"
            )

            assertEquals(1, result.actionRequiredCount)
            assertEquals(1, result.waitingOnOthersCount)
            assertEquals(1, result.disputedCount)
        }
    }
}
