package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [ReceiptExtractionService].
 * Handles capability verification and fallback strategies.
 */
internal class ReceiptExtractionServiceImpl(
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreReceiptParser: AICoreReceiptParser,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ReceiptExtractionService {

    override suspend fun extract(rawText: RawReceiptText): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        if (!aiCoreCapabilityProvider.isSupported()) {
            return@withContext Result.success(getNoOpFallbackReceipt())
        }

        aiCoreReceiptParser.parse(rawText).recover {
            getNoOpFallbackReceipt()
        }
    }

    override fun capability(): ExtractionCapability {
        return if (aiCoreCapabilityProvider.isSupported()) {
            ExtractionCapability.ON_DEVICE_AI
        } else {
            ExtractionCapability.UNSUPPORTED
        }
    }

    private fun getNoOpFallbackReceipt(): ExtractedReceipt {
        return ExtractedReceipt(
            amount = null,
            currency = null,
            date = null,
            title = null,
            source = ExtractionSource.NO_OP,
            confidence = ExtractionConfidence.LOW
        )
    }
}
