package es.pedrazamiguez.splittrip.features.expense.presentation.model

/** Add-on row in the extras section of the expense detail screen. */
data class AddOnDetailUiModel(
    val labelText: String,
    val modeText: String,
    val formattedAmount: String,
    /** True for DISCOUNT add-ons — drives negative/green styling. */
    val isDiscount: Boolean = false
)
