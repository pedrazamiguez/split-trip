package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface ExtractReceiptFieldsUseCase : UseCase {
    suspend operator fun invoke(attachment: ReceiptAttachment): Result<ExtractedReceipt>
}
