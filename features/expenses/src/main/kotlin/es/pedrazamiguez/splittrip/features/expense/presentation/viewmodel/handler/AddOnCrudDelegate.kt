package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.util.UUID

/**
 * Stateless delegate that handles add-on creation and payment method switching logic.
 *
 * Extracted from [AddOnEventHandler] to reduce method length (Detekt `LongMethod`)
 * while keeping the logic testable.
 */
class AddOnCrudDelegate(
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper,
    private val exchangeRateDelegate: AddOnExchangeRateDelegate
) {

    /**
     * Builds a new [AddOnUiModel] for the given [type], pre-configured with the
     * current expense's currency, payment method, and exchange rate settings.
     */
    fun buildNewAddOn(type: AddOnType, state: AddExpenseUiState): AddOnUiModel {
        val groupCurrency = state.groupCurrency
        val addOnCurrency = state.selectedCurrency
        val isForeign = addOnCurrency != null &&
            groupCurrency != null &&
            addOnCurrency.code != groupCurrency.code

        val exchangeRateLabel = buildExchangeRateLabel(isForeign, groupCurrency, addOnCurrency)
        val groupAmountLabel = buildGroupAmountLabel(isForeign, groupCurrency)
        val isCash = exchangeRateDelegate.isCashMethod(state.selectedPaymentMethod?.id)
        val shouldLockRate = isForeign && isCash

        return AddOnUiModel(
            id = UUID.randomUUID().toString(),
            type = type,
            mode = AddOnMode.ON_TOP,
            currency = addOnCurrency,
            paymentMethod = state.selectedPaymentMethod,
            showExchangeRateSection = isForeign,
            exchangeRateLabel = exchangeRateLabel,
            groupAmountLabel = groupAmountLabel,
            displayExchangeRate = if (isForeign) state.displayExchangeRate else "1.0",
            isExchangeRateLocked = shouldLockRate,
            isInsufficientCash = false,
            exchangeRateLockedHint = if (shouldLockRate) {
                UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
            } else {
                null
            }
        )
    }

    /**
     * Determines currency context flags for the new add-on.
     *
     * @return Triple of (isForeign, isCash, shouldLockRate)
     */
    fun resolveAddOnCurrencyContext(state: AddExpenseUiState): Triple<Boolean, Boolean, Boolean> {
        val groupCurrency = state.groupCurrency
        val addOnCurrency = state.selectedCurrency
        val isForeign = addOnCurrency != null &&
            groupCurrency != null &&
            addOnCurrency.code != groupCurrency.code
        val isCash = exchangeRateDelegate.isCashMethod(state.selectedPaymentMethod?.id)
        val shouldLockRate = isForeign && isCash
        return Triple(isForeign, isCash, shouldLockRate)
    }

    /**
     * Applies the payment method change on an add-on, returning the updated model
     * with appropriate exchange rate lock/unlock and rate restoration.
     *
     * @return Updated [AddOnUiModel]. If no exchange-rate-related updates are needed, the
     * original model is returned unchanged.
     */
    fun applyPaymentMethodSwitch(
        addOn: AddOnUiModel,
        isCash: Boolean,
        isForeign: Boolean,
        wasCashLocked: Boolean
    ): AddOnUiModel = when {
        isCash && isForeign -> addOn.copy(
            preCashExchangeRate = addOn.displayExchangeRate,
            isExchangeRateLocked = true,
            isInsufficientCash = false,
            exchangeRateLockedHint = UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
        )
        !isCash && isForeign && wasCashLocked -> {
            val savedRate = addOn.preCashExchangeRate
            addOn.copy(
                isExchangeRateLocked = false,
                isInsufficientCash = false,
                exchangeRateLockedHint = null,
                displayExchangeRate = savedRate ?: addOn.displayExchangeRate,
                preCashExchangeRate = if (savedRate != null) null else addOn.preCashExchangeRate
            )
        }
        !isCash && wasCashLocked -> addOn.copy(
            isExchangeRateLocked = false,
            isInsufficientCash = false,
            exchangeRateLockedHint = null
        )
        else -> addOn
    }

    /**
     * Returns whether the saved pre-cash rate exists and should trigger
     * a forward recalculation instead of a fresh API fetch.
     */
    fun hasSavedPreCashRate(addOn: AddOnUiModel): Boolean =
        addOn.preCashExchangeRate != null

    // ── Label helpers ────────────────────────────────────────────────────

    internal fun buildExchangeRateLabel(
        isForeign: Boolean,
        groupCurrency: CurrencyUiModel?,
        addOnCurrency: CurrencyUiModel?
    ): String {
        if (!isForeign || groupCurrency == null || addOnCurrency == null) return ""
        return addExpenseOptionsMapper.buildExchangeRateLabel(groupCurrency, addOnCurrency)
    }

    internal fun buildGroupAmountLabel(
        isForeign: Boolean,
        groupCurrency: CurrencyUiModel?
    ): String {
        if (!isForeign || groupCurrency == null) return ""
        return addExpenseOptionsMapper.buildGroupAmountLabel(groupCurrency)
    }
}
