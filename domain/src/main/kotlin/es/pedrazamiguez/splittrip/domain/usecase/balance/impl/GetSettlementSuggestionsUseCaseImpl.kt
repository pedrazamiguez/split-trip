package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

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
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.strategy.StandardContributionAttributionStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.MemberBalanceCalculationInputs
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.first

class GetSettlementSuggestionsUseCaseImpl(
    private val debtSimplificationService: DebtSimplificationService,
    private val settlementRepository: SettlementRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val subunitRepository: SubunitRepository,
    private val cashTransferRepository: CashTransferRepository,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
) : GetSettlementSuggestionsUseCase {

    override operator fun invoke(memberBalances: List<MemberBalance>): List<Settlement> =
        debtSimplificationService.simplify(memberBalances)

    override fun invokeByPocket(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> =
        debtSimplificationService.simplifyByPocket(memberBalances, groupCurrency)

    override fun getEphemeralSettlements(
        groupId: String,
        memberBalances: List<MemberBalance>,
        groupCurrency: String,
        persistedRecords: List<SettlementRecord>
    ): List<SettlementRecord> {
        val computedSettlements = debtSimplificationService.simplifyByPocket(memberBalances, groupCurrency)
            .filter { it.sourcePocket != SettlementPocketType.NET }

        val activePersisted = persistedRecords.filter { it.status != SettlementStatus.RESOLVED }

        val ephemeralRecords = computedSettlements.mapNotNull { settlement ->
            val existing = activePersisted.find { existing ->
                existing.settlement.fromUserId == settlement.fromUserId &&
                    existing.settlement.toUserId == settlement.toUserId &&
                    existing.settlement.sourcePocket == settlement.sourcePocket &&
                    existing.settlement.currency == settlement.currency
            }
            if (existing != null) {
                null
            } else {
                SettlementRecord(
                    id = EphemeralSettlementIdHelper.createId(
                        fromUserId = settlement.fromUserId,
                        toUserId = settlement.toUserId,
                        sourcePocket = settlement.sourcePocket,
                        currency = settlement.currency,
                        amount = settlement.amount
                    ),
                    groupId = groupId,
                    settlement = settlement,
                    status = SettlementStatus.SUGGESTED,
                    createdAt = LocalDateTime.now()
                )
            }
        }

        return activePersisted + ephemeralRecords
    }

    override suspend fun persistForGroup(groupId: String, leavingUserId: String?): List<SettlementRecord> {
        val group = groupRepository.getGroupById(groupId)
            ?: return emptyList()

        val expenses = expenseRepository.getGroupExpensesFlow(groupId).first()
        val contributions = contributionRepository.getGroupContributionsFlow(groupId).first()
        val withdrawals = cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId).first()
        val subunits = subunitRepository.getGroupSubunits(groupId)
        val cashTransfers = cashTransferRepository.observeGroupCashTransfers(groupId).first()

        val existingRecords = settlementRepository.getGroupSettlements(groupId)

        val memberBalances = getMemberBalancesFlowUseCase.computeMemberBalances(
            MemberBalanceCalculationInputs(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                cashTransfers = cashTransfers,
                subunits = subunits,
                groupMemberIds = group.members,
                groupCurrency = group.currency,
                settlements = existingRecords,
                attributionStrategy = StandardContributionAttributionStrategy
            )
        )

        val computedSettlements = if (leavingUserId != null) {
            calculateLeaveSettlements(leavingUserId, memberBalances, group.currency)
        } else {
            debtSimplificationService.simplifyByPocket(
                memberBalances,
                group.currency
            ).filter { it.sourcePocket != SettlementPocketType.NET }
        }

        reconcileSettlements(groupId, computedSettlements, existingRecords)
        purgeObsoleteSuggested(computedSettlements, existingRecords)

        return settlementRepository.getGroupSettlements(groupId)
    }

    private fun calculateLeaveSettlements(
        leavingUserId: String,
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> {
        val leavingMember = memberBalances.find { it.userId == leavingUserId } ?: return emptyList()
        val remainingMembers = memberBalances.filter { it.userId != leavingUserId }
        if (remainingMembers.isEmpty()) return emptyList()

        val amount = leavingMember.pocketBalance
        if (amount == 0L) return emptyList()

        val absAmount = if (amount < 0) -amount else amount
        val eachAmount = absAmount / remainingMembers.size
        val remainder = (absAmount % remainingMembers.size).toInt()

        val settlements = mutableListOf<Settlement>()
        for (i in remainingMembers.indices) {
            val member = remainingMembers[i]
            val memberAmount = eachAmount + if (i < remainder) 1L else 0L
            if (memberAmount == 0L) continue

            if (amount > 0L) {
                settlements.add(
                    Settlement(
                        fromUserId = member.userId,
                        toUserId = leavingUserId,
                        amount = memberAmount,
                        currency = groupCurrency,
                        sourcePocket = SettlementPocketType.POCKET
                    )
                )
            } else {
                settlements.add(
                    Settlement(
                        fromUserId = leavingUserId,
                        toUserId = member.userId,
                        amount = memberAmount,
                        currency = groupCurrency,
                        sourcePocket = SettlementPocketType.POCKET
                    )
                )
            }
        }
        return settlements
    }

    private suspend fun reconcileSettlements(
        groupId: String,
        computedSettlements: List<Settlement>,
        existingRecords: List<SettlementRecord>
    ) {
        val deletedIds = deduplicateExistingRecords(existingRecords)
        val activeRecords = existingRecords.filter { it.id !in deletedIds }

        for (settlement in computedSettlements) {
            val existing = activeRecords.find { existing ->
                existing.status != SettlementStatus.RESOLVED &&
                    existing.settlement.fromUserId == settlement.fromUserId &&
                    existing.settlement.toUserId == settlement.toUserId &&
                    existing.settlement.sourcePocket == settlement.sourcePocket &&
                    existing.settlement.currency == settlement.currency
            }

            if (existing != null) {
                if (existing.status == SettlementStatus.SUGGESTED && existing.settlement != settlement) {
                    settlementRepository.updateSettlement(existing.copy(settlement = settlement))
                } else if (existing.status == SettlementStatus.DISPUTED && existing.settlement != settlement) {
                    settlementRepository.updateSettlement(
                        existing.copy(
                            settlement = settlement,
                            status = SettlementStatus.SUGGESTED,
                            disputedBy = null,
                            disputeReason = null
                        )
                    )
                }
                continue
            }

            val newRecord = SettlementRecord(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                settlement = settlement,
                status = SettlementStatus.SUGGESTED,
                createdAt = LocalDateTime.now()
            )
            settlementRepository.addSettlement(newRecord)
        }
    }

    private suspend fun deduplicateExistingRecords(existingRecords: List<SettlementRecord>): Set<String> {
        val existingByKey = existingRecords.groupBy { record ->
            SettlementKey(
                fromUserId = record.settlement.fromUserId,
                toUserId = record.settlement.toUserId,
                sourcePocket = record.settlement.sourcePocket,
                currency = record.settlement.currency
            )
        }
        val deletedIds = mutableSetOf<String>()

        for ((_, records) in existingByKey) {
            val uncompleted = records.filter { it.status != SettlementStatus.RESOLVED }
            if (uncompleted.size > 1) {
                val primary = uncompleted.firstOrNull { it.status != SettlementStatus.SUGGESTED }
                    ?: uncompleted.first()
                val duplicates = uncompleted.filter { it.id != primary.id && it.status == SettlementStatus.SUGGESTED }
                for (dup in duplicates) {
                    settlementRepository.deleteSettlement(dup)
                    deletedIds.add(dup.id)
                }
            }
        }
        return deletedIds
    }

    private data class SettlementKey(
        val fromUserId: String,
        val toUserId: String,
        val sourcePocket: SettlementPocketType,
        val currency: String
    )

    private suspend fun purgeObsoleteSuggested(
        computedSettlements: List<Settlement>,
        existingRecords: List<SettlementRecord>
    ) {
        val obsoleteSuggested = existingRecords.filter { existing ->
            existing.status == SettlementStatus.SUGGESTED &&
                computedSettlements.none { comp ->
                    comp.fromUserId == existing.settlement.fromUserId &&
                        comp.toUserId == existing.settlement.toUserId &&
                        comp.sourcePocket == existing.settlement.sourcePocket &&
                        comp.currency == existing.settlement.currency
                }
        }
        for (staleRecord in obsoleteSuggested) {
            settlementRepository.deleteSettlement(staleRecord)
        }
    }
}
