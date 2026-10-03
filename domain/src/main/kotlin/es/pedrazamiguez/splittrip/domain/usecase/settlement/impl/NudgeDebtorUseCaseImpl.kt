package es.pedrazamiguez.splittrip.domain.usecase.settlement.impl

import es.pedrazamiguez.splittrip.domain.repository.SettlementNudgeRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase

class NudgeDebtorUseCaseImpl(
    private val settlementRepository: SettlementRepository,
    private val settlementNudgeRepository: SettlementNudgeRepository,
    private val appConfigService: AppConfigService,
    private val authenticationService: AuthenticationService
) : NudgeDebtorUseCase {

    override suspend fun invoke(groupId: String, settlementId: String): Result<Unit> {
        val currentUserId = authenticationService.currentUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val (creditorId, amountCents, currency) = if (EphemeralSettlementIdHelper.isEphemeral(settlementId)) {
            val settlement = EphemeralSettlementIdHelper.parse(settlementId)
                ?: return Result.failure(IllegalArgumentException("Invalid ephemeral settlement ID"))
            Triple(settlement.toUserId, settlement.amount, settlement.currency)
        } else {
            val settlementRecord = settlementRepository.getSettlementById(settlementId)
                ?: return Result.failure(IllegalArgumentException("Settlement not found"))
            Triple(
                settlementRecord.settlement.toUserId,
                settlementRecord.settlement.amount,
                settlementRecord.settlement.currency
            )
        }

        if (creditorId != currentUserId) {
            return Result.failure(IllegalStateException("User is not the creditor for this settlement"))
        }

        val lastTimestamp = settlementNudgeRepository.getLastNudgeTimestamp(settlementId)
        val rateLimitHours = appConfigService.settlementNudgeRateLimitHours.value
        val rateLimitMillis = rateLimitHours * MILLIS_PER_HOUR
        val currentTime = System.currentTimeMillis()

        if (lastTimestamp > 0 && (currentTime - lastTimestamp) < rateLimitMillis) {
            return Result.failure(IllegalStateException("Nudge rate limit exceeded"))
        }

        val result = settlementNudgeRepository.sendDebtorNudge(
            groupId = groupId,
            settlementId = settlementId,
            amountCents = amountCents,
            currency = currency
        )
        if (result.isSuccess) {
            settlementNudgeRepository.recordNudgeTimestamp(settlementId, currentTime)
        }
        return result
    }

    private companion object {
        private const val MILLIS_PER_HOUR = 3_600_000L
    }
}
