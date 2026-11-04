package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.features.balance.R

enum class BalanceMetricType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
) {
    REMAINING(
        titleRes = R.string.balances_metric_remaining_title,
        descriptionRes = R.string.balances_metric_remaining_description
    ),
    AVAILABLE(
        titleRes = R.string.balances_metric_available_title,
        descriptionRes = R.string.balances_metric_available_description
    ),
    SCHEDULED(
        titleRes = R.string.balances_metric_scheduled_title,
        descriptionRes = R.string.balances_metric_scheduled_description
    ),
    REFUNDABLE(
        titleRes = R.string.balances_metric_refundable_title,
        descriptionRes = R.string.balances_metric_refundable_description
    ),
    TOTAL_CONTRIBUTED(
        titleRes = R.string.balances_metric_total_contributed_title,
        descriptionRes = R.string.balances_metric_total_contributed_description
    ),
    TOTAL_SPENT(
        titleRes = R.string.balances_metric_total_spent_title,
        descriptionRes = R.string.balances_metric_total_spent_description
    ),
    CASH_IN_HAND(
        titleRes = R.string.balances_metric_cash_in_hand_title,
        descriptionRes = R.string.balances_metric_cash_in_hand_description
    ),
    EXTRAS(
        titleRes = R.string.balances_metric_extras_title,
        descriptionRes = R.string.balances_metric_extras_description
    )
}
