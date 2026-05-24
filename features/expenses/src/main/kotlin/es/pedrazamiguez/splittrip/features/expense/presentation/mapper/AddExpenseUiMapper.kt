package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Core Add Expense mapper: handles date formatting, display formatting,
 * and the full UI-state → domain [Expense] conversion.
 *
 * Option-list mapping (currencies, payment methods, categories, split types)
 * is in [AddExpenseOptionsUiMapper]. Split-display and split-domain mapping is in
 * [AddExpenseSplitUiMapper]. Add-on mapping is in [AddExpenseAddOnUiMapper].
 * This class delegates to both inside [mapToDomain].
 */
class AddExpenseUiMapper(
    private val localeProvider: LocaleProvider,
    @Suppress("UnusedPrivateMember")
    private val resourceProvider: ResourceProvider,
    private val splitMapper: AddExpenseSplitUiMapper,
    private val addOnMapper: AddExpenseAddOnUiMapper,
    private val splitPreviewService: SplitPreviewService
) {

    companion object {
        private const val RATE_PRECISION = 6
    }

    // ── Date Formatting ────────────────────────────────────────────────────

    /**
     * Formats a due date millis value to a locale-aware display string.
     */
    fun formatDueDateForDisplay(dateMillis: Long): String {
        val locale = localeProvider.getCurrentLocale()
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(dateMillis),
            ZoneOffset.UTC
        )
        val formatter = java.time.format.DateTimeFormatter
            .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            .withLocale(locale)
        return dateTime.format(formatter)
    }

    // ── UI State → Domain Mapping ──────────────────────────────────────────

    // Sequential field-by-field parsing of UiState → domain model;
    // each field requires its own null-coalescing and conversion logic
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun mapToDomain(state: AddExpenseUiState, groupId: String): Result<Expense> = try {
        val sourceCurrencyCode = state.selectedCurrency?.code
        val groupCurrencyCode = state.groupCurrency?.code
        val sourceDecimalDigits = state.selectedCurrency?.decimalDigits ?: 2
        val groupDecimalDigits = state.groupCurrency?.decimalDigits ?: 2

        val sourceAmount = splitPreviewService.parseAmountToCents(state.sourceAmount, sourceDecimalDigits)

        val normalizedDisplayRate =
            CurrencyConverter.normalizeAmountString(state.displayExchangeRate.trim())
        val displayRate = normalizedDisplayRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        val internalRate = if (displayRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal.ONE.divide(displayRate, RATE_PRECISION, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val groupAmount = if (state.calculatedGroupAmount.isNotBlank()) {
            splitPreviewService.parseAmountToCents(state.calculatedGroupAmount, groupDecimalDigits)
        } else {
            BigDecimal(sourceAmount).multiply(internalRate).setScale(0, RoundingMode.HALF_UP).toLong()
        }

        val paymentMethod = state.selectedPaymentMethod?.let {
            PaymentMethod.fromString(it.id)
        } ?: PaymentMethod.CASH

        val category = state.selectedCategory?.let {
            runCatching { ExpenseCategory.fromString(it.id) }.getOrDefault(ExpenseCategory.OTHER)
        } ?: ExpenseCategory.OTHER

        val paymentStatus = state.selectedPaymentStatus?.let {
            runCatching { PaymentStatus.fromString(it.id) }.getOrDefault(PaymentStatus.FINISHED)
        } ?: PaymentStatus.FINISHED

        val dueDate = if (paymentStatus == PaymentStatus.SCHEDULED && state.dueDateMillis != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(state.dueDateMillis), ZoneOffset.UTC)
        } else {
            null
        }

        val splitType = state.selectedSplitType?.let { SplitType.fromString(it.id) } ?: SplitType.EQUAL

        val splits = if (state.isSubunitMode && state.entitySplits.isNotEmpty()) {
            splitMapper.mapEntitySplitsToDomain(state.entitySplits, splitType)
        } else {
            splitMapper.mapSplitsToDomain(state.splits, splitType)
        }

        val addOns = addOnMapper.mapAddOnsToDomain(
            state.addOns,
            sourceCurrencyCode ?: groupCurrencyCode ?: "EUR"
        )

        val payerType = state.selectedFundingSource?.id?.let {
            runCatching { PayerType.fromString(it) }.getOrDefault(PayerType.GROUP)
        } ?: PayerType.GROUP

        val payerId = if (payerType == PayerType.USER) state.currentUserId else null
        val createdAt = state.expenseDateMillis?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
        }

        val expense = Expense(
            groupId = groupId,
            title = state.expenseTitle,
            sourceAmount = sourceAmount,
            sourceCurrency = sourceCurrencyCode ?: "EUR",
            groupAmount = groupAmount,
            groupCurrency = groupCurrencyCode ?: "EUR",
            exchangeRate = internalRate,
            addOns = addOns,
            category = category,
            vendor = state.vendor.ifBlank { null },
            notes = state.notes.ifBlank { null },
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            dueDate = dueDate,
            receiptAttachment = state.receiptAttachment,
            splitType = splitType,
            splits = splits,
            payerType = payerType,
            payerId = payerId,
            createdAt = createdAt
        )
        Result.success(expense)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
