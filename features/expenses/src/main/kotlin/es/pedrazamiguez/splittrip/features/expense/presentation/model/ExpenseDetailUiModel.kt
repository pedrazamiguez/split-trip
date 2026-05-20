package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExpenseDetailUiModel(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val categoryText: String = "",

    // Amount
    val formattedGroupAmount: String = "",
    val groupCurrency: String = "EUR",
    val formattedSourceAmount: String? = null,
    val sourceCurrency: String = "EUR",
    /** Formatted as "1 X = Y Z"; null when same-currency. */
    val formattedExchangeRate: String? = null,
    val isForeignCurrency: Boolean = false,

    // Metadata
    val paymentMethodText: String = "",
    val paymentMethodIcon: ImageVector = TablerIcons.Outline.CreditCard,
    val paymentStatusText: String = "",
    val paymentStatusIcon: ImageVector = TablerIcons.Outline.CircleCheck,
    /** Contextual role of this expense — e.g. "group expense", "personal expense". */
    val expenseScopeLabel: String = "",
    val paidByText: String = "",
    val dateText: String = "",
    val vendorText: String? = null,
    val notesText: String? = null,
    val scheduledBadgeText: String? = null,
    val isScheduledPastDue: Boolean = false,

    // Funding source
    val isOutOfPocket: Boolean = false,
    val fundingSourceText: String? = null,

    // Split
    val splitTypeText: String = "",
    val splits: ImmutableList<SplitDetailUiModel> = persistentListOf(),
    /**
     * Subunit-grouped splits. When non-empty, the screen renders these as collapsible
     * containers above any solo entries in [splits]. Solo expenses (no subunit splits)
     * keep this empty and render only [splits].
     */
    val splitGroups: ImmutableList<SubunitSplitGroupUiModel> = persistentListOf(),

    // Add-ons
    val hasAddOns: Boolean = false,
    val hasIncludedAddOns: Boolean = false,
    val addOns: ImmutableList<AddOnDetailUiModel> = persistentListOf(),
    val formattedEffectiveTotal: String? = null,
    /**
     * Formatted decomposed base cost when INCLUDED add-ons are present (equal to the
     * stored `groupAmount` — what the user "really" paid for the item before the
     * embedded tip/fee). Null when no INCLUDED add-ons exist.
     */
    val formattedIncludedBaseCost: String? = null,
    /**
     * Formatted total the user originally entered, reconstructed from the base cost
     * plus INCLUDED add-ons. Null when no INCLUDED add-ons exist.
     */
    val formattedOriginalEnteredTotal: String? = null,

    // Cash tranches
    val cashTranches: ImmutableList<CashTrancheDetailUiModel> = persistentListOf(),

    // Receipt
    val receiptUri: String? = null,
    /** MIME type of the attached receipt (e.g. `image/webp`, `application/pdf`). Null when no receipt. */
    val receiptMimeType: String? = null,

    // Provenance
    val createdByText: String = "",
    val createdAtText: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
