package es.pedrazamiguez.splittrip.core.designsystem.navigation

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding"
    const val RECONCILIATION = "reconciliation"
    const val MAIN = "main"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val GROUPS = "groups"
    const val CREATE_GROUP = "create_group"
    const val EXPENSES = "expenses"
    const val EXPENSES_FILTER = "expenses_filter"
    const val ADD_EXPENSE = "add_expense"
    const val BALANCES = "balances"
    const val YOUR_BALANCE = "your_balance"
    const val YOUR_POSITION = YOUR_BALANCE
    const val CATEGORY_SPENDING = "category_spending"
    const val CONTRIBUTION_WIZARD_ARG_GROUP_ID = "groupId"
    const val CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID = "contributionId"
    const val CONTRIBUTION_WIZARD =
        "contribution_wizard/{$CONTRIBUTION_WIZARD_ARG_GROUP_ID}" +
            "?$CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID={$CONTRIBUTION_WIZARD_ARG_CONTRIBUTION_ID}"
    const val CONTRIBUTION_DETAIL_ARG_GROUP_ID = "groupId"
    const val CONTRIBUTION_DETAIL_ARG_CONTRIBUTION_ID = "contributionId"
    const val CONTRIBUTION_DETAIL =
        "contribution_detail/{$CONTRIBUTION_DETAIL_ARG_GROUP_ID}/{$CONTRIBUTION_DETAIL_ARG_CONTRIBUTION_ID}"
    const val ADD_CASH_WITHDRAWAL = "add_cash_withdrawal"
    const val SETTINGS = "settings"
    const val SETTINGS_DEFAULT_CURRENCY = "settings_default_currency"
    const val SETTINGS_LANGUAGE = "settings_language"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_DEVELOPER_SERVICES = "settings_developer_services"
    const val SETTINGS_THEME = "settings_theme"
    const val SETTINGS_ACCOUNT_STATUS = "settings_account_status"
    const val SETTINGS_SUBSCRIPTIONS = "settings_subscriptions"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_FAQ = "settings_faq"
    const val SETTINGS_PRIVACY_POLICY = "settings_privacy_policy"
    const val SETTINGS_OPEN_SOURCE = "settings_open_source"
    const val SETTINGS_DEVELOPER_INFO = "settings_developer_info"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val EDIT_GROUP = "edit_group/{groupId}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val MANAGE_SUBUNITS = "manage_subunits/{groupId}"
    const val CREATE_EDIT_SUBUNIT = "create_edit_subunit/{groupId}?subunitId={subunitId}"
    const val GROUP_SETTLEMENT_OVERVIEW = "group_settlement_overview/{groupId}"
    const val RECEIPT_VIEWER = "receipt_viewer/{receiptUri}?mimeType={mimeType}"

    fun groupSettlementOverviewRoute(groupId: String) = "group_settlement_overview/$groupId"

    fun groupDetailRoute(groupId: String) = "group_detail/$groupId"

    fun editGroupRoute(groupId: String) = "edit_group/$groupId"

    fun expenseDetailRoute(expenseId: String) = "expense_detail/$expenseId"

    fun expensesFilterRoute() = EXPENSES_FILTER

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

    fun contributionWizardRoute(groupId: String, contributionId: String? = null): String {
        val base = "contribution_wizard/$groupId"
        return if (contributionId != null) "$base?contributionId=$contributionId" else base
    }

    fun contributionDetailRoute(groupId: String, contributionId: String): String =
        "contribution_detail/$groupId/$contributionId"
}
