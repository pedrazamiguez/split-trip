package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DownloadReceiptUseCase : UseCase {
    suspend operator fun invoke(expenseId: String, remoteUrl: String): Result<ReceiptAttachment>
}
