package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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

    /**
     * Formats an expense date millis value to a locale-aware display string (medium date + short time).
     */
    fun formatExpenseDateForDisplay(dateMillis: Long): String {
        val locale = localeProvider.getCurrentLocale()
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(dateMillis),
            ZoneOffset.UTC
        )
        val formatter = java.time.format.DateTimeFormatter
            .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)
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
            BigDecimal.ONE.divide(displayRate, DomainConstants.RATE_PRECISION, RoundingMode.HALF_UP)
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

    /**
     * Maps an existing domain [Expense] and its paired [Contribution] back into UI state for modification.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun mapExpenseToState(
        expense: Expense,
        contribution: Contribution?,
        currentState: AddExpenseUiState,
        memberProfiles: Map<String, User>,
        subunits: List<Subunit>
    ): AddExpenseUiState {
        val groupDecimalDigits = expense.groupCurrency.let { _ ->
            currentState.groupCurrency?.decimalDigits ?: 2
        }

        val sourceDecimalDigits = expense.sourceCurrency.let { code ->
            currentState.availableCurrencies.find { it.code == code }?.decimalDigits ?: 2
        }
        val sourceAmountString = BigDecimal(expense.sourceAmount).movePointLeft(sourceDecimalDigits)
            .stripTrailingZeros().toPlainString()
        val calculatedGroupAmountString = BigDecimal(
            expense.groupAmount
        ).movePointLeft(groupDecimalDigits).stripTrailingZeros().toPlainString()

        val selectedCurrency = currentState.availableCurrencies.find { it.code == expense.sourceCurrency }
        val selectedPaymentMethod = currentState.paymentMethods.find { it.id == expense.paymentMethod.name }
        val selectedFundingSource = currentState.fundingSources.find { it.id == expense.payerType.name }
        val selectedCategory = currentState.availableCategories.find { it.id == expense.category.name }
        val selectedPaymentStatus = currentState.availablePaymentStatuses.find { it.id == expense.paymentStatus.name }

        val isForeign = expense.sourceCurrency != expense.groupCurrency
        val displayRate = if (expense.exchangeRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal.ONE.divide(expense.exchangeRate, DomainConstants.RATE_PRECISION, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        } else {
            "1.0"
        }

        val formattedDueDate = expense.dueDate?.let {
            val millis = it.toInstant(ZoneOffset.UTC).toEpochMilli()
            formatDueDateForDisplay(millis)
        } ?: ""

        val dueDateMillis = expense.dueDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

        val addOnsMapped = mapAddOnsToState(
            addOns = expense.addOns,
            availableCurrencies = currentState.availableCurrencies,
            paymentMethods = currentState.paymentMethods,
            groupCurrency = expense.groupCurrency,
            groupDecimalDigits = groupDecimalDigits,
            sourceAmount = expense.sourceAmount
        )

        val (mappedSplits, mappedEntitySplits) = mapSplitsToState(
            expense = expense,
            currentState = currentState,
            memberProfiles = memberProfiles,
            subunits = subunits
        )

        val expenseDateMillis =
            expense.createdAt?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: currentState.expenseDateMillis
        val formattedExpenseDate =
            expenseDateMillis?.let { formatExpenseDateForDisplay(it) } ?: currentState.formattedExpenseDate

        val selectedSplitType = currentState.availableSplitTypes.find { it.id == expense.splitType.name }

        val contributionScope = contribution?.contributionScope ?: PayerType.USER
        val selectedContributionSubunitId = contribution?.subunitId

        return currentState.copy(
            expenseTitle = expense.title,
            sourceAmount = sourceAmountString,
            vendor = expense.vendor ?: "",
            notes = expense.notes ?: "",
            selectedCurrency = selectedCurrency,
            selectedPaymentMethod = selectedPaymentMethod,
            selectedFundingSource = selectedFundingSource,
            selectedCategory = selectedCategory,
            selectedPaymentStatus = selectedPaymentStatus,
            displayExchangeRate = displayRate,
            calculatedGroupAmount = calculatedGroupAmountString,
            showExchangeRateSection = isForeign,
            isExchangeRateLocked = expense.paymentMethod == PaymentMethod.CASH && isForeign,
            dueDateMillis = dueDateMillis,
            formattedDueDate = formattedDueDate,
            showDueDateSection = expense.paymentStatus == PaymentStatus.SCHEDULED,
            receiptUri = expense.receiptAttachment?.let { it.localUri.takeIf { it.isNotBlank() } ?: it.remoteUrl },
            receiptAttachment = expense.receiptAttachment,
            addOns = addOnsMapped.toImmutableList(),
            isSubunitMode = expense.splits.any { it.subunitId != null },
            splits = mappedSplits,
            entitySplits = mappedEntitySplits,
            contributionScope = contributionScope,
            selectedContributionSubunitId = selectedContributionSubunitId,
            expenseDateMillis = expenseDateMillis,
            formattedExpenseDate = formattedExpenseDate,
            selectedSplitType = selectedSplitType,
            isAiModeActive = false
        )
    }

    private fun mapAddOnsToState(
        addOns: List<AddOn>,
        availableCurrencies: List<CurrencyUiModel>,
        paymentMethods: List<PaymentMethodUiModel>,
        groupCurrency: String?,
        groupDecimalDigits: Int,
        sourceAmount: Long
    ): List<AddOnUiModel> {
        return addOns.map { addOn ->
            val currencyUiModel = availableCurrencies.find { it.code == addOn.currency }
            val paymentMethodUiModel = paymentMethods.find { it.id == addOn.paymentMethod.name }
            val isAddOnForeign = addOn.currency != groupCurrency

            val addOnDisplayRate = getAddOnDisplayRate(addOn.exchangeRate)
            val addOnDecimalDigits = currencyUiModel?.decimalDigits ?: 2
            val amountInput = getAddOnAmountInput(addOn, sourceAmount, addOnDecimalDigits)

            AddOnUiModel(
                id = addOn.id,
                type = addOn.type,
                mode = addOn.mode,
                valueType = addOn.valueType,
                amountInput = amountInput,
                resolvedAmountCents = addOn.amountCents,
                currency = currencyUiModel,
                displayExchangeRate = addOnDisplayRate,
                showExchangeRateSection = isAddOnForeign,
                isExchangeRateLocked = addOn.paymentMethod == PaymentMethod.CASH && isAddOnForeign,
                calculatedGroupAmount = BigDecimal(
                    addOn.groupAmountCents
                ).movePointLeft(groupDecimalDigits).toPlainString(),
                groupAmountCents = addOn.groupAmountCents,
                paymentMethod = paymentMethodUiModel,
                description = addOn.description ?: ""
            )
        }
    }

    private fun getAddOnDisplayRate(exchangeRate: BigDecimal): String {
        return if (exchangeRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal.ONE.divide(exchangeRate, DomainConstants.RATE_PRECISION, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        } else {
            "1.0"
        }
    }

    private fun getAddOnAmountInput(addOn: AddOn, sourceAmount: Long, decimalDigits: Int): String {
        if (addOn.valueType != AddOnValueType.PERCENTAGE) {
            return BigDecimal(addOn.amountCents).movePointLeft(decimalDigits).stripTrailingZeros().toPlainString()
        }

        val baseCents = if (addOn.mode == AddOnMode.INCLUDED && addOn.type == AddOnType.DISCOUNT) {
            sourceAmount + addOn.amountCents
        } else {
            sourceAmount
        }

        return if (baseCents > 0) {
            val pct = BigDecimal(addOn.amountCents * 100).divide(BigDecimal(baseCents), 2, RoundingMode.HALF_UP)
            pct.stripTrailingZeros().toPlainString()
        } else {
            ""
        }
    }

    private fun mapSplitsToState(
        expense: Expense,
        currentState: AddExpenseUiState,
        memberProfiles: Map<String, User>,
        subunits: List<Subunit>
    ): Pair<ImmutableList<SplitUiModel>, ImmutableList<SplitUiModel>> {
        val hasSubunitSplits = expense.splits.any { it.subunitId != null }

        val mappedSplits = if (hasSubunitSplits) {
            currentState.splits
        } else {
            splitMapper.mapDomainToSplits(
                memberIds = currentState.memberIds,
                shares = expense.splits,
                memberProfiles = memberProfiles
            )
        }

        val mappedEntitySplits = if (hasSubunitSplits) {
            splitMapper.buildEntitySplitsFromDomain(
                memberIds = currentState.memberIds,
                subunits = subunits,
                shares = expense.splits,
                availableSplitTypes = currentState.availableSplitTypes,
                memberProfiles = memberProfiles
            )
        } else {
            currentState.entitySplits
        }

        return mappedSplits to mappedEntitySplits
    }
}
