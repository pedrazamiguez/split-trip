package es.pedrazamiguez.splittrip.features.expense.presentation.model

/** Add-on row in the breakdown section of the expense detail screen. */
data class AddOnDetailUiModel(
    val labelText: String,
    val modeText: String,
    /** Always formatted in the **group** currency — the value that hits balances. */
    val formattedAmount: String,
    /**
     * Formatted amount in the add-on's **own** currency (e.g. "£5.00").
     * Null when the add-on shares the group currency.
     */
    val formattedSourceAmount: String? = null,
    /** ISO code of the add-on's own currency. */
    val addOnCurrency: String = "",
    /**
     * Locked exchange rate from the add-on currency to the group currency, formatted
     * for display (e.g. "1 GBP = 1.20 EUR"). Null when same-currency.
     */
    val formattedRate: String? = null,
    /** True when the add-on currency differs from the group currency. */
    val isForeignCurrency: Boolean = false,
    /** True when the add-on is INCLUDED in the user-entered total (vs. ON_TOP). */
    val isIncluded: Boolean = false,
    /** True for DISCOUNT add-ons — drives negative/success styling. */
    val isDiscount: Boolean = false
)
