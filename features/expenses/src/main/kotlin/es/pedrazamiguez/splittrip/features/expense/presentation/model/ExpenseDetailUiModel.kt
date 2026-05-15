package es.pedrazamiguez.splittrip.features.expense.presentation.model

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
    val paymentStatusText: String = "",
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

    // Add-ons
    val hasAddOns: Boolean = false,
    val addOns: ImmutableList<AddOnDetailUiModel> = persistentListOf(),
    val formattedEffectiveTotal: String? = null,

    // Cash tranches
    val cashTranches: ImmutableList<CashTrancheDetailUiModel> = persistentListOf(),

    // Receipt
    val receiptUri: String? = null,

    // Provenance
    val createdByText: String = "",
    val createdAtText: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
