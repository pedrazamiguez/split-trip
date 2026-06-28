package es.pedrazamiguez.splittrip.features.expense.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus

data class ExpenseUiModel(
    val id: String = "",
    val title: String = "",
    val formattedAmount: String = "",
    val formattedOriginalAmount: String? = null,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val categoryText: String = "",
    val vendorText: String? = null,
    val paymentMethodText: String = "",
    val paymentStatusText: String = "",
    val paidByText: String = "",
    val dateText: String = "",
    /**
     * Badge text for SCHEDULED expenses. Null when not applicable.
     * - Future: "Due on 15 Mar"
     * - Today:  "Due today"
     * - Past:   "Paid"
     */
    val scheduledBadgeText: String? = null,
    /**
     * True when the scheduled payment's due date has passed or is today,
     * used to pick the check icon (✅) vs clock icon (🕐) in the UI.
     */
    val isScheduledPastDue: Boolean = false,
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
    val isCancelled: Boolean = false
)
