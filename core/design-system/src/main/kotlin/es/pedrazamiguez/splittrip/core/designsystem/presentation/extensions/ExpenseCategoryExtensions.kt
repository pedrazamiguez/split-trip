package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import es.pedrazamiguez.splittrip.core.common.R
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory

fun ExpenseCategory.toStringRes(): Int = when (this) {
    ExpenseCategory.TRANSPORT -> R.string.expense_category_transport
    ExpenseCategory.FOOD -> R.string.expense_category_food
    ExpenseCategory.LODGING -> R.string.expense_category_lodging
    ExpenseCategory.HOME -> R.string.expense_category_home
    ExpenseCategory.HEALTH_PERSONAL_CARE -> R.string.expense_category_health_personal_care
    ExpenseCategory.ACTIVITIES -> R.string.expense_category_activities
    ExpenseCategory.INSURANCE -> R.string.expense_category_insurance
    ExpenseCategory.ENTERTAINMENT -> R.string.expense_category_entertainment
    ExpenseCategory.SHOPPING -> R.string.expense_category_shopping
    ExpenseCategory.OTHER -> R.string.expense_category_other
}
