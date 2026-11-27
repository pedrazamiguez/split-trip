package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.LocalAiPackBannerUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

class LocalAiPackBannerUiMapper(
    private val localeProvider: LocaleProvider
) {

    fun map(packState: LocalAiModelPackState): LocalAiPackBannerUiModel? {
        if (!packState.isOptInBannerEligible) return null
        val notInstalledStatus = packState.status as? ModelPackStatus.NotInstalled ?: return null
        val sizeBytes = packState.catalogEntries
            .firstOrNull { it.isRecommended }
            ?.config
            ?.sizeBytes
            ?: notInstalledStatus.downloadSizeBytes

        return LocalAiPackBannerUiModel(
            title = UiText.StringResource(R.string.expense_local_ai_banner_enable_title),
            description = UiText.StringResource(R.string.expense_local_ai_banner_enable_description),
            sizeBadge = formatSizeBadge(sizeBytes),
            actionLabel = UiText.StringResource(R.string.expense_local_ai_banner_action)
        )
    }

    private fun formatSizeBadge(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        val numberFormat = NumberFormat.getNumberInstance(localeProvider.getCurrentLocale()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }
        return if (safeBytes >= BYTES_PER_GB) {
            val gbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_GB), 1, RoundingMode.HALF_UP)
            "${numberFormat.format(gbValue)} GB"
        } else {
            val mbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_MB), 1, RoundingMode.HALF_UP)
            "${numberFormat.format(mbValue)} MB"
        }
    }

    private companion object {
        private const val BYTES_PER_MB = 1_048_576L
        private const val BYTES_PER_GB = 1_073_741_824L
    }
}
