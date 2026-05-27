package es.pedrazamiguez.splittrip.core.designsystem.navigation

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val PROFILE = "profile"
    const val GROUPS = "groups"
    const val CREATE_GROUP = "create_group"
    const val EXPENSES = "expenses"
    const val ADD_EXPENSE = "add_expense"
    const val BALANCES = "balances"
    const val ADD_CONTRIBUTION = "add_contribution"
    const val ADD_CASH_WITHDRAWAL = "add_cash_withdrawal"
    const val SETTINGS = "settings"
    const val SETTINGS_DEFAULT_CURRENCY = "settings_default_currency"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_DEVELOPER_SERVICES = "settings_developer_services"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val MANAGE_SUBUNITS = "manage_subunits/{groupId}"
    const val CREATE_EDIT_SUBUNIT = "create_edit_subunit/{groupId}?subunitId={subunitId}"
    const val RECEIPT_VIEWER = "receipt_viewer/{receiptUri}?mimeType={mimeType}"

    fun groupDetailRoute(groupId: String) = "group_detail/$groupId"

    fun expenseDetailRoute(expenseId: String) = "expense_detail/$expenseId"

    fun editExpenseRoute(expenseId: String) = "edit_expense/$expenseId"

    fun manageSubunitsRoute(groupId: String) = "manage_subunits/$groupId"

    fun createEditSubunitRoute(groupId: String, subunitId: String? = null): String {
        val base = "create_edit_subunit/$groupId"
        return if (subunitId != null) "$base?subunitId=$subunitId" else base
    }

    fun receiptViewerRoute(receiptUri: String, mimeType: String? = null): String {
        val encodedUri = Uri.encode(receiptUri)
        val base = "receipt_viewer/$encodedUri"
        return if (mimeType != null) "$base?mimeType=${Uri.encode(mimeType)}" else base
    }
}
