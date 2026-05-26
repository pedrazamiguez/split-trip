package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.model.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.service.AiModelResolver
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class ReceiptExtractionServiceImpl(
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreReceiptParser: Lazy<AICoreReceiptParser>,
    private val aiModelResolver: AiModelResolver,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ReceiptExtractionService {

    override suspend fun extract(
        rawText: RawReceiptText,
        engineType: AiEngineType?
    ): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        val resolvedEngine = engineType ?: aiModelResolver.getActiveModel().first()
        Timber.d(
            "ReceiptExtractionService: resolvedEngine=%s — extracting",
            resolvedEngine.name
        )

        when (resolvedEngine) {
            AiEngineType.AI_CORE_GEMMA_4 -> extractWithAiCore(rawText)
            AiEngineType.LITE_RT_LM -> extractWithLiteRtLm(engineType)
        }
    }

    private suspend fun extractWithAiCore(rawText: RawReceiptText): Result<ExtractedReceipt> {
        val cap = aiCoreCapabilityProvider.isSupported()
        Timber.d(
            "ReceiptExtractionService (AICore): capability=%s — %s",
            if (cap) "ON_DEVICE_AI" else "UNSUPPORTED",
            if (cap) "delegating to AICoreReceiptParser" else "returning NO_OP fallback immediately"
        )

        if (!cap) {
            return Result.success(getNoOpFallbackReceipt())
        }

        val startMs = System.currentTimeMillis()
        val result = aiCoreReceiptParser.value.parse(rawText).recover { error ->
            Timber.w(
                error,
                "ReceiptExtractionService: AICore parser failed mid-flight — " +
                    "falling back to NO_OP (elapsed=%dms)",
                System.currentTimeMillis() - startMs
            )
            getNoOpFallbackReceipt()
        }
        Timber.d(
            "ReceiptExtractionService: extraction finished in %dms — source=%s",
            System.currentTimeMillis() - startMs,
            result.getOrNull()?.source
        )
        return result
    }

    private suspend fun extractWithLiteRtLm(engineType: AiEngineType?): Result<ExtractedReceipt> {
        val isDeveloperOverride = aiModelResolver.getDeveloperOverrideModel().first() == AiEngineType.LITE_RT_LM
        return if (isDeveloperOverride || engineType == AiEngineType.LITE_RT_LM) {
            Result.success(getLiteRTLmFallbackReceipt())
        } else {
            Result.success(getNoOpFallbackReceipt())
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
            time = null,
            title = null,
            vendor = null,
            category = null,
            paymentMethod = null,
            notes = null,
            source = ExtractionSource.NO_OP,
            confidence = ExtractionConfidence.LOW
        )
    }

    @Suppress("MagicNumber")
    private fun getLiteRTLmFallbackReceipt(): ExtractedReceipt {
        return ExtractedReceipt(
            amount = BigDecimal("42.50"),
            currency = "EUR",
            date = LocalDate.now(),
            time = LocalTime.of(12, 0),
            title = "Mock LiteRT-LM Receipt",
            vendor = "LiteRT Store",
            category = "SHOPPING",
            paymentMethod = "CREDIT_CARD",
            notes = "Processed via mock LiteRT-LM engine",
            source = ExtractionSource.LITE_RT_LM,
            confidence = ExtractionConfidence.MEDIUM
        )
    }
}
