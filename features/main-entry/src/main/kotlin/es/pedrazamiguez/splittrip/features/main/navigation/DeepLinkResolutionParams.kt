package es.pedrazamiguez.splittrip.features.main.navigation

/**
 * Extracted deep link parameters used for target tab resolution.
 */
data class DeepLinkResolutionParams(
    val expenseId: String? = null,
    val isExpensesListPath: Boolean = false,
    val contributionId: String? = null,
    val withdrawalId: String? = null,
    val settlementId: String? = null,
    val isYourPositionPath: Boolean = false,
    val isGroupsListPath: Boolean = false,
    val isMembersPath: Boolean = false
)
