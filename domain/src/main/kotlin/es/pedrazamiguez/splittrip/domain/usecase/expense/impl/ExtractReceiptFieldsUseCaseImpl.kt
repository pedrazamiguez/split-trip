package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import java.time.LocalDate
import java.time.YearMonth

class ExtractReceiptFieldsUseCaseImpl(
    private val ocrService: ReceiptOcrService,
    private val extractionService: ReceiptExtractionService,
    private val appConfigService: AppConfigService,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val yearMonthProvider: () -> YearMonth = { YearMonth.now() }
) : ExtractReceiptFieldsUseCase {

    /**
     * OCRs the provided [attachment] and extracts structured fields from the raw text.
     *
     * @param attachment The receipt attachment to process.
     * @return A [Result] enclosing the [ExtractedReceipt] on success.
     */
    override suspend operator fun invoke(attachment: ReceiptAttachment): Result<ExtractedReceipt> {
        return ocrService.recogniseText(attachment).fold(
            onSuccess = { rawText ->
                extractionService.extract(rawText).map { result ->
                    userPreferenceRepository.incrementMonthlyAiReceiptScanCount(yearMonthProvider())
                    val maxFutureDays = appConfigService.extractedDateMaxFutureDays.value.toLong()
                    val thresholdDate = LocalDate.now().plusDays(maxFutureDays)
                    if (result.date != null && result.date.isAfter(thresholdDate)) {
                        result.copy(date = LocalDate.now())
                    } else {
                        result
                    }
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
