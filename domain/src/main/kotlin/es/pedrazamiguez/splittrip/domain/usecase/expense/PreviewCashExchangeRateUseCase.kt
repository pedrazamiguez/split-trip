package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface PreviewCashExchangeRateUseCase : UseCase {
    suspend operator fun invoke(
        groupId: String,
        sourceCurrency: String,
        sourceAmountCents: Long,
        payerType: PayerType = PayerType.GROUP,
        payerId: String? = null,
        preferredWithdrawalScope: PayerType? = null,
        preferredWithdrawalOwnerId: String? = null
    ): CashRatePreviewResult
}
