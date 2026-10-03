package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.strategy.CashSettlementPaymentStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.strategy.PocketSettlementPaymentStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.strategy.SettlementConfirmationStrategyFactory
import java.time.LocalDateTime
import java.util.UUID

class ConfirmSettlementUseCaseImpl(
    private val settlementRepository: SettlementRepository,
    private val authenticationService: AuthenticationService,
    private val groupRepository: GroupRepository,
    private val contributionRepository: ContributionRepository,
    private val groupDashboardDataSource: GroupDashboardDataSource,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
) : ConfirmSettlementUseCase {

    @Suppress("LongMethod")
    override suspend operator fun invoke(
        groupId: String,
        settlementId: String
    ): Result<SettlementRecord> = runCatching {
        val currentUserId = authenticationService.requireUserId()
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found: $groupId")
        val isCreator = group.createdBy == currentUserId

        val (record, isNewMaterialization) = if (EphemeralSettlementIdHelper.isEphemeral(settlementId)) {
            val settlement = EphemeralSettlementIdHelper.parse(settlementId)
                ?: throw IllegalArgumentException("Invalid ephemeral settlement ID: $settlementId")
            val existing = settlementRepository.getGroupSettlements(groupId).find {
                it.status != SettlementStatus.RESOLVED &&
                    it.settlement.fromUserId == settlement.fromUserId &&
                    it.settlement.toUserId == settlement.toUserId &&
                    it.settlement.sourcePocket == settlement.sourcePocket &&
                    it.settlement.currency == settlement.currency
            }
            if (existing != null) {
                existing to false
            } else {
                SettlementRecord(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    settlement = settlement,
                    status = SettlementStatus.SUGGESTED,
                    createdAt = LocalDateTime.now()
                ) to true
            }
        } else {
            val found = settlementRepository.getSettlementById(settlementId)
                ?: throw IllegalArgumentException("Settlement not found: $settlementId")
            found to false
        }

        val strategy = SettlementConfirmationStrategyFactory.getStrategy(record)
        val updated = strategy.confirm(record, currentUserId, isCreator)

        if (isNewMaterialization) {
            settlementRepository.addSettlement(updated)
        } else {
            settlementRepository.updateSettlement(updated)
        }

        if (updated.status == SettlementStatus.RESOLVED) {
            handleResolvedSettlement(
                record = record,
                updated = updated,
                group = group,
                groupId = groupId,
                currentUserId = currentUserId
            )
        }

        updated
    }

    private suspend fun handleResolvedSettlement(
        record: SettlementRecord,
        updated: SettlementRecord,
        group: Group,
        groupId: String,
        currentUserId: String
    ) {
        val strategies = listOf(
            PocketSettlementPaymentStrategy(
                contributionRepository = contributionRepository,
                groupDashboardDataSource = groupDashboardDataSource,
                getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase
            ),
            CashSettlementPaymentStrategy()
        )

        val strategy = strategies.firstOrNull { it.appliesTo(record.settlement.sourcePocket) }
            ?: throw IllegalArgumentException(
                "No payment strategy found for pocket type: ${record.settlement.sourcePocket}"
            )

        strategy.processPayment(record, updated, group, groupId, currentUserId)
    }
}
