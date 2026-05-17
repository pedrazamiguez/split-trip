package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.extension.resolveLocalizedName
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.CashTranchePreview
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toFundingSourceStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTranchePreviewUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.WithdrawalPoolOptionUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps enum / domain option lists to their UI-model counterparts for the Add Expense form.
 *
 * Responsible for:
 * - [Currency] → [CurrencyUiModel]
 * - [PaymentMethod] / [ExpenseCategory] / [PaymentStatus] / [SplitType] → selector UI models
 * - Exchange-rate and group-amount label strings
 *
 * Extracted from [AddExpenseUiMapper] to keep class function count within the
 * configured Detekt threshold.
 */
class AddExpenseOptionsUiMapper(
    private val resourceProvider: ResourceProvider,
    private val formattingHelper: FormattingHelper
) {

    fun mapCurrency(currency: Currency): CurrencyUiModel = CurrencyUiModel(
        code = currency.code,
        displayText = currency.formatDisplay(),
        decimalDigits = currency.decimalDigits,
        defaultName = currency.defaultName,
        localizedName = currency.resolveLocalizedName(resourceProvider)
    )

    fun mapCurrencies(currencies: List<Currency>): ImmutableList<CurrencyUiModel> =
        currencies.map { mapCurrency(it) }.toImmutableList()

    fun mapPaymentMethods(methods: List<PaymentMethod>): ImmutableList<PaymentMethodUiModel> =
        methods.map { method ->
            PaymentMethodUiModel(
                id = method.name,
                displayText = resourceProvider.getString(method.toStringRes()),
                icon = method.toIconVector()
            )
        }.toImmutableList()

    /**
     * Maps a list of user-selectable [PayerType] values to UI models.
     * Only GROUP and USER are user-selectable; SUBUNIT is excluded.
     */
    fun mapFundingSources(payerTypes: List<PayerType>): ImmutableList<FundingSourceUiModel> =
        payerTypes
            .filter { it != PayerType.SUBUNIT }
            .map { payerType ->
                FundingSourceUiModel(
                    id = payerType.name,
                    displayText = resourceProvider.getString(payerType.toFundingSourceStringRes())
                )
            }.toImmutableList()

    /**
     * Maps a list of [ExpenseCategory] enums to UI models, filtering out
     * non-user-selectable categories (CONTRIBUTION, REFUND).
     */
    fun mapCategories(categories: List<ExpenseCategory>): ImmutableList<CategoryUiModel> =
        categories
            .filter { it != ExpenseCategory.CONTRIBUTION && it != ExpenseCategory.REFUND }
            .map { category ->
                CategoryUiModel(
                    id = category.name,
                    displayText = resourceProvider.getString(category.toStringRes())
                )
            }.toImmutableList()

    /**
     * Maps a list of [PaymentStatus] enums to UI models, filtering to only
     * user-selectable statuses (FINISHED, SCHEDULED).
     */
    fun mapPaymentStatuses(statuses: List<PaymentStatus>): ImmutableList<PaymentStatusUiModel> =
        statuses
            .filter { it == PaymentStatus.FINISHED || it == PaymentStatus.SCHEDULED }
            .map { status ->
                PaymentStatusUiModel(
                    id = status.name,
                    displayText = resourceProvider.getString(status.toStringRes())
                )
            }.toImmutableList()

    fun mapSplitTypes(splitTypes: List<SplitType>): ImmutableList<SplitTypeUiModel> =
        splitTypes.map { splitType ->
            SplitTypeUiModel(
                id = splitType.name,
                displayText = resourceProvider.getString(splitType.toStringRes())
            )
        }.toImmutableList()

    fun buildExchangeRateLabel(
        groupCurrency: CurrencyUiModel,
        selectedCurrency: CurrencyUiModel
    ): String = resourceProvider.getString(
        R.string.add_expense_rate_label_format,
        groupCurrency.displayText,
        selectedCurrency.displayText
    )

    fun buildGroupAmountLabel(groupCurrency: CurrencyUiModel): String =
        resourceProvider.getString(R.string.add_expense_amount_in, groupCurrency.displayText)

    /**
     * Maps a list of [CashTranchePreview] domain models to UI models for the "Funded from"
     * breakdown in the Exchange Rate step.
     *
     * Label resolution:
     * - Uses [CashTranchePreview.withdrawalTitle] if non-blank.
     * - Falls back to `"ATM — <formatted date>"` using [CashTranchePreview.withdrawalDate].
     * - When date is also null, falls back to just `"ATM"`.
     *
     * @param tranches         The tranche list from [CashRatePreview.tranches].
     * @param sourceCurrencyCode ISO 4217 code of the source currency for formatting amounts.
     * @return Immutable list of [CashTranchePreviewUiModel] in FIFO order.
     */
    fun mapCashTranchePreviews(
        tranches: List<CashTranchePreview>,
        sourceCurrencyCode: String
    ): ImmutableList<CashTranchePreviewUiModel> =
        tranches.map { tranche ->
            val label = if (!tranche.withdrawalTitle.isNullOrBlank()) {
                tranche.withdrawalTitle.orEmpty()
            } else {
                val formattedDate = formattingHelper.formatShortDate(tranche.withdrawalDate)
                if (formattedDate.isNotBlank()) {
                    resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label, formattedDate)
                } else {
                    resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label_no_date)
                }
            }

            val formattedConsumed = formattingHelper.formatCentsWithCurrency(
                tranche.amountConsumedCents,
                sourceCurrencyCode
            )
            val formattedRemaining = formattingHelper.formatCentsWithCurrency(
                tranche.remainingAfterCents,
                sourceCurrencyCode
            )
            val formattedRate = formattingHelper.formatRateForDisplay(
                tranche.withdrawalRate.toPlainString()
            )

            CashTranchePreviewUiModel(
                withdrawalLabel = label,
                formattedAmountConsumed = formattedConsumed,
                formattedRemainingAfter = formattedRemaining,
                formattedRate = formattedRate
            )
        }.toImmutableList()

    /**
     * Maps a list of [WithdrawalPoolOption] domain models to UI models for the pool-selection
     * widget shown in the Exchange Rate step when multiple pools have available funds.
     *
     * Label resolution by scope:
     * - **USER:** "My personal cash" (localized string resource).
     * - **GROUP:** "Group cash" (localized string resource).
     * - **SUBUNIT:** "[subunit name] cash" — looked up from [subunitNameLookup] by [WithdrawalPoolOption.ownerId],
     *   falling back to a generic "Subunit cash" label when the name is not found.
     *
     * @param pools             The available pool options returned by [GetAvailableWithdrawalPoolsUseCase].
     * @param subunitNameLookup Map of subunitId → subunit display name. Provide from
     *                          [AddExpenseUiState.contributionSubunitOptions] converted to a map.
     * @return Immutable list of [WithdrawalPoolOptionUiModel] in the same order as [pools].
     */
    fun mapWithdrawalPoolOptions(
        pools: List<WithdrawalPoolOption>,
        subunitNameLookup: Map<String, String> = emptyMap()
    ): ImmutableList<WithdrawalPoolOptionUiModel> =
        pools.map { pool ->
            val label = when (pool.scope) {
                PayerType.USER -> resourceProvider.getString(R.string.add_expense_cash_pool_personal)
                PayerType.GROUP -> resourceProvider.getString(R.string.add_expense_cash_pool_group)
                PayerType.SUBUNIT -> {
                    val subunitName = pool.ownerId?.let { subunitNameLookup[it] }
                    if (!subunitName.isNullOrBlank()) {
                        resourceProvider.getString(R.string.add_expense_cash_pool_subunit, subunitName)
                    } else {
                        resourceProvider.getString(R.string.add_expense_cash_pool_subunit_generic)
                    }
                }
            }
            WithdrawalPoolOptionUiModel(
                scope = pool.scope,
                ownerId = pool.ownerId,
                displayLabel = label
            )
        }.toImmutableList()
}
