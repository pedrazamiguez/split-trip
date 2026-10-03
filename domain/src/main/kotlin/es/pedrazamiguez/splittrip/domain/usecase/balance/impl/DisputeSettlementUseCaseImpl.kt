package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import java.time.LocalDateTime
import java.util.UUID

class DisputeSettlementUseCaseImpl(
    private val settlementRepository: SettlementRepository,
    private val authenticationService: AuthenticationService
) : DisputeSettlementUseCase {

    override suspend operator fun invoke(
        groupId: String,
        settlementId: String,
        reason: String
    ): Result<SettlementRecord> = runCatching {
        val currentUserId = authenticationService.requireUserId()
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

        val isPayer = record.settlement.fromUserId == currentUserId
        val isPayee = record.settlement.toUserId == currentUserId
        require(isPayer || isPayee) { "Only settlement parties can dispute" }

        require(record.status != SettlementStatus.RESOLVED) {
            "Cannot dispute a resolved settlement: $settlementId"
        }
        require(record.status != SettlementStatus.DISPUTED) {
            "Settlement already disputed: $settlementId"
        }

        val updated = record.copy(
            status = SettlementStatus.DISPUTED,
            disputedBy = currentUserId,
            disputeReason = reason
        )

        if (isNewMaterialization) {
            settlementRepository.addSettlement(updated)
        } else {
            settlementRepository.updateSettlement(updated)
        }
        updated
    }
}
