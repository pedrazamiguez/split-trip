package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface AttachReceiptUseCase : UseCase {
    suspend operator fun invoke(sourceUri: String): Result<ReceiptAttachment>
}
