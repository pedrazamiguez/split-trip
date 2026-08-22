package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus

data class ExpenseUiModel(
    val id: String = "",
    val title: String = "",
    val formattedAmount: String = "",
    val formattedOriginalAmount: String? = null,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val categoryText: String = "",
    val subcategory: ExpenseSubcategory = ExpenseSubcategory.UNSPECIFIED,
    val subcategoryText: String? = null,
    val vendorText: String? = null,
    val paymentMethodText: String = "",
    val paymentStatusText: String = "",
    val paidByText: String = "",
    val creatorDisplay: MemberDisplay = MemberDisplay.Active("", ""),
    val dateText: String = "",
    /**
     * Text for the dynamic payment status badge (e.g., "Yesterday", "15 Mar").
     * Null when no badge should be shown.
     */
    val badgeText: String? = null,
    /**
     * The icon for the dynamic payment status badge.
     * Evaluated based on the PaymentStatus and whether the date has passed.
     */
    val badgeIcon: ImageVector? = null,
    /**
     * True when the badge represents an urgent state (e.g. due today).
     * Used for styling.
     */
    val isBadgeUrgent: Boolean = false,
    /**
     * True when the expense has add-ons (fees, tips, surcharges, discounts).
     * Used to display an indicator badge in the expense list item.
     */
    val hasAddOns: Boolean = false,
    /**
     * True when the expense was paid from a member's personal money
     * (payerType == PayerType.USER). Used to display an out-of-pocket badge.
     */
    val isOutOfPocket: Boolean = false,
    /**
     * Resolved funding source text for out-of-pocket expenses.
     * Scope-aware: e.g., "Paid by me", "Paid by María", "Paid for Cantalobos",
     * "Paid by María for everyone". Null when the expense is group-funded.
     */
    val fundingSourceText: String? = null,
    /**
     * True when the paired contribution's scope is SUBUNIT.
     * Used to pick the Group icon in the out-of-pocket badge.
     */
    val isSubunitScope: Boolean = false,
    /**
     * True when the paired contribution's scope is GROUP.
     * Used to pick the Groups icon in the out-of-pocket badge.
     */
    val isGroupScope: Boolean = false,
    /**
     * Cloud synchronization status of this expense.
     * Drives the [SyncStatusIndicator] visibility in the list item.
     */
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    /**
     * True when the expense status is CANCELLED.
     * Used for strikethrough formatting and background fading.
     */
    val isCancelled: Boolean = false,
    /**
     * True when the expense is a refundable reservation.
     * Used to show the "Cancel & Refund" action in the long-press menu.
     */
    val isRefundable: Boolean = false,
    /**
     * True when the expense is composed of multiple payment tranches.
     */
    val isComposite: Boolean = false,
    /**
     * Number of sub-expenses / tranches in this composite expense.
     */
    val subExpenseCount: Int = 0,
    /**
     * Percentage of the total group amount that has been paid (0-100).
     */
    val paidPercentage: Int = 100
)
