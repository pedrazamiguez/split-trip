package es.pedrazamiguez.splittrip.features.expense.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType

/**
 * UI model representing a single add-on in the expense form.
 *
 * [amountInput] is the raw user input (editable text field).
 * [resolvedAmountCents] is the computed absolute amount in add-on currency cents.
 * [groupAmountCents] is the amount converted to the group currency.
 */
data class AddOnUiModel(
    val id: String,
    val type: AddOnType = AddOnType.FEE,
    val mode: AddOnMode = AddOnMode.ON_TOP,
    val valueType: AddOnValueType = AddOnValueType.EXACT,

    /** Raw user input in the amount field (locale-aware string). */
    val amountInput: String = "",

    /** Resolved absolute amount in the add-on's own currency (cents). */
    val resolvedAmountCents: Long = 0L,

    /** The add-on's currency (defaults to expense source currency). */
    val currency: CurrencyUiModel? = null,

    /** Display exchange rate: "1 GroupCurrency = X AddOnCurrency". */
    val displayExchangeRate: String = "1.0",

    /** Whether to show the exchange rate section for this add-on. */
    val showExchangeRateSection: Boolean = false,

    /**
     * True when the exchange rate is determined by ATM withdrawal rates (CASH payment)
     * and should not be editable by the user.
     */
    val isExchangeRateLocked: Boolean = false,

    /**
     * Informational message explaining why the rate is locked.
     * Shown in the exchange rate section when [isExchangeRateLocked] is true.
     */
    val exchangeRateLockedHint: UiText? = null,

    /**
     * True when the available cash cannot cover the add-on amount.
     * Drives error styling on the locked hint text.
     */
    val isInsufficientCash: Boolean = false,

    /**
     * True when the exchange rate was served from an expired local cache
     * (the remote API was unreachable). Drives a warning indicator.
     */
    val isExchangeRateStale: Boolean = false,

    /**
     * Snapshot of [displayExchangeRate] taken just before switching to CASH payment.
     * Restored when the user switches back to a non-CASH method.
     */
    val preCashExchangeRate: String? = null,

    /** Pre-formatted label for the rate field (e.g., "1 EUR (€) = ? THB (฿)"). */
    val exchangeRateLabel: String = "",

    /** Pre-formatted label for the converted group amount field (e.g., "Amount in EUR (€)"). */
    val groupAmountLabel: String = "",

    /** Calculated group amount string for display in the conversion card. */
    val calculatedGroupAmount: String = "",

    /** Whether a rate fetch is in progress for this add-on. */
    val isLoadingRate: Boolean = false,

    /** Amount converted to group currency (cents). */
    val groupAmountCents: Long = 0L,

    /** Payment method override (defaults to expense payment method). */
    val paymentMethod: PaymentMethodUiModel? = null,

    /** Optional free-text description. */
    val description: String = "",

    /** Whether the amount input is valid. */
    val isAmountValid: Boolean = true,

    /** Whether this add-on card is expanded in the UI. */
    val isExpanded: Boolean = false,

    /** Pre-formatted amount string in group currency for display in summary cards. */
    val formattedAmount: String = ""
)
