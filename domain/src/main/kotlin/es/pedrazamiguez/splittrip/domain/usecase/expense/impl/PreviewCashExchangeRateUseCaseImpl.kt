package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashRatePreview
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.model.CashTranchePreview
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import java.math.BigDecimal
import java.math.RoundingMode

class PreviewCashExchangeRateUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val exchangeRateCalculationService: ExchangeRateCalculationService
) : PreviewCashExchangeRateUseCase {

    companion object {
        private const val RATE_PRECISION = 6
    }

    override suspend operator fun invoke(
        groupId: String,
        sourceCurrency: String,
        sourceAmountCents: Long,
        payerType: PayerType,
        payerId: String?,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): CashRatePreviewResult {
        val withdrawals = if (preferredWithdrawalScope != null) {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = sourceCurrency,
                scope = preferredWithdrawalScope,
                scopeOwnerId = preferredWithdrawalOwnerId
            )
        } else {
            cashWithdrawalRepository.getAvailableWithdrawals(
                groupId,
                sourceCurrency,
                payerType,
                payerId
            )
        }
        if (withdrawals.isEmpty()) return CashRatePreviewResult.NoWithdrawals
        if (sourceAmountCents <= 0) return previewWithoutAmount(withdrawals)
        return previewWithAmount(sourceAmountCents, withdrawals)
    }

    /**
     * No amount entered yet — return weighted-average display rate from all withdrawals.
     * For a single withdrawal, use its stored exchange rate directly to avoid
     * integer-cent rounding artefacts.
     */
    private fun previewWithoutAmount(
        withdrawals: List<CashWithdrawal>
    ): CashRatePreviewResult {
        if (withdrawals.size == 1) {
            val rate = withdrawals.first().exchangeRate
            if (rate > BigDecimal.ZERO) {
                return CashRatePreviewResult.Available(
                    CashRatePreview(displayRate = rate)
                )
            }
        }

        val totalWithdrawn = withdrawals.sumOf { it.amountWithdrawn }
        val totalDeducted = withdrawals.sumOf { it.deductedBaseAmount }
        if (totalWithdrawn <= 0 || totalDeducted <= 0) {
            return CashRatePreviewResult.NoWithdrawals
        }

        val weightedDisplayRate = BigDecimal(totalWithdrawn)
            .divide(BigDecimal(totalDeducted), RATE_PRECISION, RoundingMode.HALF_UP)
        return CashRatePreviewResult.Available(
            CashRatePreview(displayRate = weightedDisplayRate)
        )
    }

    /**
     * Amount entered — simulate FIFO to get the blended group amount and display rate.
     */
    private fun previewWithAmount(
        sourceAmountCents: Long,
        withdrawals: List<CashWithdrawal>
    ): CashRatePreviewResult {
        if (expenseCalculatorService.hasInsufficientCash(sourceAmountCents, withdrawals)) {
            return CashRatePreviewResult.InsufficientCash
        }

        // Simulate FIFO to get the blended group amount
        val fifoResult = expenseCalculatorService.calculateFifoCashAmount(
            amountToCover = sourceAmountCents,
            availableWithdrawals = withdrawals
        )

        // When the entire expense falls within a single withdrawal tranche,
        // use that withdrawal's stored exchange rate directly. This avoids the
        // display rate fluctuating due to integer-cent rounding in the FIFO
        // group amount calculation.
        val displayRate = if (fifoResult.tranches.size == 1) {
            val tranche = fifoResult.tranches.first()
            val withdrawal = withdrawals.find { it.id == tranche.withdrawalId }
            val storedRate = withdrawal?.exchangeRate ?: BigDecimal.ZERO
            if (storedRate > BigDecimal.ZERO) {
                storedRate
            } else {
                exchangeRateCalculationService.calculateBlendedDisplayRate(
                    sourceAmountCents = sourceAmountCents,
                    groupAmountCents = fifoResult.groupAmountCents
                )
            }
        } else {
            exchangeRateCalculationService.calculateBlendedDisplayRate(
                sourceAmountCents = sourceAmountCents,
                groupAmountCents = fifoResult.groupAmountCents
            )
        }

        val withdrawalsById = withdrawals.associateBy { it.id }
        val tranchePreviews = fifoResult.tranches.mapNotNull { tranche ->
            // FIFO tranches must always reference the provided withdrawals list;
            // a missing entry signals a data inconsistency — skip the tranche.
            val withdrawal = withdrawalsById[tranche.withdrawalId] ?: return@mapNotNull null
            CashTranchePreview(
                withdrawalId = tranche.withdrawalId,
                withdrawalTitle = withdrawal.title,
                withdrawalDate = withdrawal.createdAt,
                amountConsumedCents = tranche.amountConsumed,
                remainingAfterCents = withdrawal.remainingAmount - tranche.amountConsumed,
                withdrawalRate = withdrawal.exchangeRate
            )
        }

        return CashRatePreviewResult.Available(
            CashRatePreview(
                displayRate = displayRate,
                groupAmountCents = fifoResult.groupAmountCents,
                tranches = tranchePreviews
            )
        )
    }
}
