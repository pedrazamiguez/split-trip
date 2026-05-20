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
import timber.log.Timber

internal class ReceiptExtractionServiceImpl(
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreReceiptParser: AICoreReceiptParser,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ReceiptExtractionService {

    override suspend fun extract(rawText: RawReceiptText): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        val cap = aiCoreCapabilityProvider.isSupported()
        Timber.d(
            "ReceiptExtractionService: capability=%s — %s",
            if (cap) "ON_DEVICE_AI" else "UNSUPPORTED",
            if (cap) "delegating to AICoreReceiptParser" else "returning NO_OP fallback immediately"
        )

        if (!cap) {
            return@withContext Result.success(getNoOpFallbackReceipt())
        }

        val startMs = System.currentTimeMillis()
        val result = aiCoreReceiptParser.parse(rawText).recover { error ->
            Timber.w(
                error,
                "ReceiptExtractionService: AICore parser failed mid-flight — falling back to NO_OP (elapsed=%dms)",
                System.currentTimeMillis() - startMs
            )
            getNoOpFallbackReceipt()
        }
        Timber.d(
            "ReceiptExtractionService: extraction finished in %dms — source=%s",
            System.currentTimeMillis() - startMs,
            result.getOrNull()?.source
        )
        result
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
