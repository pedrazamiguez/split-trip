package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase

class GetReceiptExtractionCapabilityUseCaseImpl(
    private val receiptExtractionService: ReceiptExtractionService
) : GetReceiptExtractionCapabilityUseCase {

    override fun invoke(): ExtractionCapability = receiptExtractionService.capability()
}
