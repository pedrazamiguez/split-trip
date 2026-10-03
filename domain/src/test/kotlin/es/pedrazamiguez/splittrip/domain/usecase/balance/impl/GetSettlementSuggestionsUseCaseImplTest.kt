package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetSettlementSuggestionsUseCaseImplTest {

    private val debtSimplificationService = mockk<DebtSimplificationService>()
    private val settlementRepository = mockk<SettlementRepository>()
    private val groupRepository = mockk<GroupRepository>()
    private val expenseRepository = mockk<ExpenseRepository>()
    private val contributionRepository = mockk<ContributionRepository>()
    private val cashWithdrawalRepository = mockk<CashWithdrawalRepository>()
    private val subunitRepository = mockk<SubunitRepository>()
    private val cashTransferRepository = mockk<CashTransferRepository>()
    private val getMemberBalancesFlowUseCase = mockk<GetMemberBalancesFlowUseCase>()
    private val useCase = GetSettlementSuggestionsUseCaseImpl(
        debtSimplificationService = debtSimplificationService,
        settlementRepository = settlementRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        contributionRepository = contributionRepository,
        cashWithdrawalRepository = cashWithdrawalRepository,
        subunitRepository = subunitRepository,
        cashTransferRepository = cashTransferRepository,
        getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase
    )

    private val groupId = "group-1"
    private val memberBalances = listOf(
        MemberBalance(userId = "1", pocketBalance = -1000, cashInHand = 0),
        MemberBalance(userId = "2", pocketBalance = 1000, cashInHand = 0)
    )

    private fun mockBaseFlowRepos(currency: String = "EUR") {
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("user-1", "user-2")
            every { this@apply.currency } returns currency
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(emptyList())
        coEvery {
            getMemberBalancesFlowUseCase.computeMemberBalances(any())
        } returns
            memberBalances
    }

    @Test
    fun `invoke delegates to DebtSimplificationService`() {
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "2", pocketBalance = 1000, cashInHand = 0)
        )
        val expected = listOf(Settlement(fromUserId = "1", toUserId = "2", amount = 1000L))
        every { debtSimplificationService.simplify(balances) } returns expected

        val result = useCase(balances)

        assertEquals(expected, result)
        verify(exactly = 1) { debtSimplificationService.simplify(balances) }
    }

    @Test
    fun `invokeByPocket delegates to DebtSimplificationService simplifyByPocket with groupCurrency`() {
        val balances = listOf(
            MemberBalance(userId = "1", pocketBalance = 500, cashInHand = -300),
            MemberBalance(userId = "2", pocketBalance = -500, cashInHand = 300)
        )
        val expected = listOf(Settlement(fromUserId = "1", toUserId = "2", amount = 200L))
        every { debtSimplificationService.simplifyByPocket(balances, "EUR") } returns expected
        val result = useCase.invokeByPocket(balances, "EUR")

        assertEquals(expected, result)
        verify(exactly = 1) { debtSimplificationService.simplifyByPocket(balances, "EUR") }
    }

    @Test
    fun `persistForGroup filters out NET and persists only CASH settlements`() = runTest {
        val cashSettlement =
            Settlement(
                fromUserId = "2",
                toUserId = "1",
                amount = 500L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        val netSettlement =
            Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 1000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.NET
            )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns
            listOf(netSettlement, cashSettlement)
        mockBaseFlowRepos()

        val expectedRecord =
            SettlementRecord(
                id = "new-1",
                groupId = groupId,
                settlement = cashSettlement,
                status = SettlementStatus.SUGGESTED,
                createdAt = LocalDateTime.now()
            )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returnsMany
            listOf(emptyList(), listOf(expectedRecord))
        coEvery { settlementRepository.addSettlement(any()) } returns Unit

        val result = useCase.persistForGroup(groupId)

        coVerify(exactly = 1) {
            settlementRepository.addSettlement(
                match {
                    it.settlement.sourcePocket ==
                        SettlementPocketType.CASH
                }
            )
        }
        coVerify(exactly = 0) {
            settlementRepository.addSettlement(
                match {
                    it.settlement.sourcePocket ==
                        SettlementPocketType.NET
                }
            )
        }
        assertEquals(1, result.size)
        assertEquals(SettlementStatus.SUGGESTED, result.first().status)
    }

    @Test
    fun `persistForGroup skips pairs with existing non-RESOLVED records`() = runTest {
        val existingSettlement =
            Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 1000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(existingSettlement)
        mockBaseFlowRepos()

        val existingRecord =
            SettlementRecord(
                id = "existing-1",
                groupId = groupId,
                settlement = existingSettlement,
                status = SettlementStatus.CONFIRMED_BY_PAYER,
                createdAt = LocalDateTime.now()
            )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returns listOf(existingRecord)

        useCase.persistForGroup(groupId)

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
    }

    @Test
    fun `persistForGroup updates existing SUGGESTED record when details changed`() = runTest {
        val existingSettlement =
            Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 1000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        val computedSettlement = existingSettlement.copy(amount = 1500L)
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(computedSettlement)
        mockBaseFlowRepos()

        val existingRecord =
            SettlementRecord(
                id = "existing-1",
                groupId = groupId,
                settlement = existingSettlement,
                status = SettlementStatus.SUGGESTED,
                createdAt = LocalDateTime.now()
            )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returnsMany listOf(
            listOf(existingRecord),
            listOf(existingRecord.copy(settlement = computedSettlement))
        )
        coEvery { settlementRepository.updateSettlement(any()) } returns Unit

        val result = useCase.persistForGroup(groupId)

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
        coVerify(exactly = 1) {
            settlementRepository.updateSettlement(
                match {
                    it.id == "existing-1" && it.settlement.amount == 1500L
                }
            )
        }
        assertEquals(1, result.size)
        assertEquals(1500L, result.first().settlement.amount)
    }

    @Test
    fun `persistForGroup resets existing DISPUTED record when details changed`() = runTest {
        val existingSettlement =
            Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 1000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        val computedSettlement = existingSettlement.copy(amount = 1500L)
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(computedSettlement)
        mockBaseFlowRepos()

        val existingRecord =
            SettlementRecord(
                id = "existing-1",
                groupId = groupId,
                settlement = existingSettlement,
                status = SettlementStatus.DISPUTED,
                createdAt = LocalDateTime.now(),
                disputedBy = "1",
                disputeReason = "Wrong amount"
            )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returnsMany listOf(
            listOf(existingRecord),
            listOf(
                existingRecord.copy(
                    settlement = computedSettlement,
                    status = SettlementStatus.SUGGESTED,
                    disputedBy = null,
                    disputeReason = null
                )
            )
        )
        coEvery { settlementRepository.updateSettlement(any()) } returns Unit

        val result = useCase.persistForGroup(groupId)

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
        coVerify(exactly = 1) {
            settlementRepository.updateSettlement(
                match {
                    it.id == "existing-1" &&
                        it.settlement.amount == 1500L &&
                        it.status == SettlementStatus.SUGGESTED &&
                        it.disputedBy == null &&
                        it.disputeReason == null
                }
            )
        }
        assertEquals(1, result.size)
        assertEquals(1500L, result.first().settlement.amount)
        assertEquals(SettlementStatus.SUGGESTED, result.first().status)
    }

    @Test
    fun `persistForGroup skips existing SUGGESTED record when details did not change`() = runTest {
        val existingSettlement =
            Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 1000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(existingSettlement)
        mockBaseFlowRepos()

        val existingRecord =
            SettlementRecord(
                id = "existing-1",
                groupId = groupId,
                settlement = existingSettlement,
                status = SettlementStatus.SUGGESTED,
                createdAt = LocalDateTime.now()
            )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returns listOf(existingRecord)

        useCase.persistForGroup(groupId)

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
        coVerify(exactly = 0) { settlementRepository.updateSettlement(any()) }
    }

    @Test
    fun `persistForGroup returns empty list when group not found`() = runTest {
        coEvery { groupRepository.getGroupById("missing-group") } returns null

        val result = useCase.persistForGroup("missing-group")

        assertEquals(emptyList<SettlementRecord>(), result)
    }

    @Test
    fun `persistForGroup purges obsolete SUGGESTED records when no longer computed`() = runTest {
        val obsoleteRecord = SettlementRecord(
            id = "stale-1",
            groupId = groupId,
            settlement = Settlement(
                fromUserId = "1",
                toUserId = "2",
                amount = 500L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.CASH
            ),
            status = SettlementStatus.SUGGESTED,
            createdAt = LocalDateTime.now()
        )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns emptyList()
        mockBaseFlowRepos()

        coEvery { settlementRepository.getGroupSettlements(groupId) } returnsMany listOf(
            listOf(obsoleteRecord),
            emptyList()
        )
        coEvery { settlementRepository.deleteSettlement(any()) } returns Unit

        val result = useCase.persistForGroup(groupId)

        coVerify(exactly = 1) { settlementRepository.deleteSettlement(obsoleteRecord) }
        assertEquals(0, result.size)
    }

    @Test
    fun `persistForGroup purges duplicate SUGGESTED records with same business key`() = runTest {
        val settlement = Settlement(
            fromUserId = "1",
            toUserId = "2",
            amount = 1000L,
            currency = "EUR",
            sourcePocket = SettlementPocketType.POCKET
        )
        val primaryRecord = SettlementRecord(
            id = "rec-1",
            groupId = groupId,
            settlement = settlement,
            status = SettlementStatus.SUGGESTED,
            createdAt = LocalDateTime.now()
        )
        val duplicateRecord = SettlementRecord(
            id = "rec-2",
            groupId = groupId,
            settlement = settlement,
            status = SettlementStatus.SUGGESTED,
            createdAt = LocalDateTime.now()
        )

        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(settlement)
        mockBaseFlowRepos()

        coEvery { settlementRepository.getGroupSettlements(groupId) } returnsMany listOf(
            listOf(primaryRecord, duplicateRecord),
            listOf(primaryRecord)
        )
        coEvery { settlementRepository.deleteSettlement(duplicateRecord) } returns Unit

        val result = useCase.persistForGroup(groupId)

        coVerify(exactly = 1) { settlementRepository.deleteSettlement(duplicateRecord) }
        assertEquals(1, result.size)
    }

    @Test
    fun `persistForGroup with leavingUserId when leaving member is owed money`() = runTest {
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("leaving-user", "member-2", "member-3")
            every { this@apply.currency } returns "EUR"
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(emptyList())

        val balances = listOf(
            MemberBalance(userId = "leaving-user", pocketBalance = 3000, cashInHand = 0), // Owed 30 EUR
            MemberBalance(userId = "member-2", pocketBalance = -1500, cashInHand = 0),
            MemberBalance(userId = "member-3", pocketBalance = -1500, cashInHand = 0)
        )
        coEvery {
            getMemberBalancesFlowUseCase.computeMemberBalances(any())
        } returns balances

        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()
        coEvery { settlementRepository.addSettlement(any()) } returns Unit

        useCase.persistForGroup(groupId, leavingUserId = "leaving-user")

        coVerify {
            settlementRepository.addSettlement(
                match {
                    it.settlement.fromUserId == "member-2" &&
                        it.settlement.toUserId == "leaving-user" &&
                        it.settlement.amount == 1500L &&
                        it.settlement.sourcePocket == SettlementPocketType.POCKET
                }
            )
            settlementRepository.addSettlement(
                match {
                    it.settlement.fromUserId == "member-3" &&
                        it.settlement.toUserId == "leaving-user" &&
                        it.settlement.amount == 1500L &&
                        it.settlement.sourcePocket == SettlementPocketType.POCKET
                }
            )
        }
    }

    @Test
    fun `persistForGroup with leavingUserId when leaving member owes money`() = runTest {
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("leaving-user", "member-2", "member-3")
            every { this@apply.currency } returns "EUR"
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(emptyList())

        val balances = listOf(
            MemberBalance(userId = "leaving-user", pocketBalance = -3000, cashInHand = 0), // Owes 30 EUR
            MemberBalance(userId = "member-2", pocketBalance = 1500, cashInHand = 0),
            MemberBalance(userId = "member-3", pocketBalance = 1500, cashInHand = 0)
        )
        coEvery {
            getMemberBalancesFlowUseCase.computeMemberBalances(any())
        } returns balances

        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()
        coEvery { settlementRepository.addSettlement(any()) } returns Unit

        useCase.persistForGroup(groupId, leavingUserId = "leaving-user")

        coVerify {
            settlementRepository.addSettlement(
                match {
                    it.settlement.fromUserId == "leaving-user" &&
                        it.settlement.toUserId == "member-2" &&
                        it.settlement.amount == 1500L &&
                        it.settlement.sourcePocket == SettlementPocketType.POCKET
                }
            )
            settlementRepository.addSettlement(
                match {
                    it.settlement.fromUserId == "leaving-user" &&
                        it.settlement.toUserId == "member-3" &&
                        it.settlement.amount == 1500L &&
                        it.settlement.sourcePocket == SettlementPocketType.POCKET
                }
            )
        }
    }

    @Test
    fun `persistForGroup with leavingUserId when leaving member pocket balance is zero`() = runTest {
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("leaving-user", "member-2")
            every { this@apply.currency } returns "EUR"
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(emptyList())

        val balances = listOf(
            MemberBalance(userId = "leaving-user", pocketBalance = 0, cashInHand = 0),
            MemberBalance(userId = "member-2", pocketBalance = 0, cashInHand = 0)
        )
        coEvery {
            getMemberBalancesFlowUseCase.computeMemberBalances(any())
        } returns balances

        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()

        useCase.persistForGroup(groupId, leavingUserId = "leaving-user")

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
    }

    @Test
    fun `persistForGroup with leavingUserId when leaving member is not in balances`() = runTest {
        mockBaseFlowRepos()
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("member-2")
            every { this@apply.currency } returns "EUR"
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = "member-2", pocketBalance = 1000, cashInHand = 0)
        )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()

        useCase.persistForGroup(groupId, leavingUserId = "non-existent")

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
    }

    @Test
    fun `persistForGroup with leavingUserId when there are no remaining members`() = runTest {
        mockBaseFlowRepos()
        val group = mockk<Group>(relaxed = true).apply {
            every { members } returns listOf("leaving-user")
            every { this@apply.currency } returns "EUR"
        }
        coEvery { groupRepository.getGroupById(groupId) } returns group
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = "leaving-user", pocketBalance = 1000, cashInHand = 0)
        )
        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()

        useCase.persistForGroup(groupId, leavingUserId = "leaving-user")

        coVerify(exactly = 0) { settlementRepository.addSettlement(any()) }
    }

    @Test
    fun `getEphemeralSettlements returns ephemeral suggestions when no active persisted records exist`() {
        val settlement = Settlement(
            fromUserId = "1",
            toUserId = "2",
            amount = 1000L,
            currency = "EUR",
            sourcePocket = SettlementPocketType.POCKET
        )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(settlement)

        val result = useCase.getEphemeralSettlements(groupId, memberBalances, "EUR", emptyList())

        assertEquals(1, result.size)
        assertEquals(SettlementStatus.SUGGESTED, result[0].status)
        assertEquals(settlement, result[0].settlement)
        assertTrue(result[0].id.startsWith("ephemeral|"))
    }

    @Test
    fun `getEphemeralSettlements preserves active persisted CONFIRMED_BY_PAYER and does not duplicate`() {
        val settlement = Settlement(
            fromUserId = "1",
            toUserId = "2",
            amount = 1000L,
            currency = "EUR",
            sourcePocket = SettlementPocketType.POCKET
        )
        val existingRecord = SettlementRecord(
            id = "persisted-1",
            groupId = groupId,
            settlement = settlement,
            status = SettlementStatus.CONFIRMED_BY_PAYER,
            createdAt = LocalDateTime.now()
        )
        every { debtSimplificationService.simplifyByPocket(memberBalances, "EUR") } returns listOf(settlement)

        val result = useCase.getEphemeralSettlements(groupId, memberBalances, "EUR", listOf(existingRecord))

        assertEquals(1, result.size)
        assertEquals("persisted-1", result[0].id)
        assertEquals(SettlementStatus.CONFIRMED_BY_PAYER, result[0].status)
    }
}
