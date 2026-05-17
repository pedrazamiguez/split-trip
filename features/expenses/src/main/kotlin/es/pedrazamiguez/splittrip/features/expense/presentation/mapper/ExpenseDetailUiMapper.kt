package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.common.util.DisplayNameResolver
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTrancheDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpenseDetailUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val addOnCalculationService: AddOnCalculationService
) {

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        withdrawalLookup: Map<String, CashWithdrawal> = emptyMap(),
        subunitNameLookup: Map<String, String> = emptyMap()
    ): ExpenseDetailUiModel {
        val (scheduledBadgeText, isScheduledPastDue) = buildScheduledBadge(expense)
        val isForeignCurrency = expense.sourceCurrency != expense.groupCurrency
        val youLabel = resourceProvider.getString(R.string.you_label)
        val paidByName = resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel)
        val paidByText = if (expense.createdBy == currentUserId) {
            resourceProvider.getString(R.string.paid_by_you)
        } else {
            resourceProvider.getString(R.string.paid_by, paidByName)
        }

        val effectiveTotal = if (expense.addOns.isNotEmpty()) {
            addOnCalculationService.calculateEffectiveGroupAmount(expense.groupAmount, expense.addOns)
        } else {
            null
        }
        val hasIncludedAddOns = expense.addOns.any { it.mode == AddOnMode.INCLUDED }
        // Base-cost extraction (adjustForIncludedAddOns in SubmitEventHandler) only runs for
        // INCLUDED non-discount add-ons. For INCLUDED DISCOUNTs the expense.groupAmount is the
        // total paid (unchanged), so there is no "base cost" or "original entered total" to
        // reconstruct — those add-ons are informational only.
        val hasIncludedNonDiscounts = expense.addOns.any {
            it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
        }
        val originalEnteredTotal = if (hasIncludedNonDiscounts) {
            buildOriginalEnteredTotal(expense.groupAmount, expense.addOns)
        } else {
            null
        }

        val (soloSplits, splitGroups) = mapSplits(expense, memberProfiles, currentUserId, youLabel, subunitNameLookup)

        return ExpenseDetailUiModel(
            id = expense.id,
            groupId = expense.groupId,
            title = expense.title,
            category = expense.category,
            categoryText = resourceProvider.getString(expense.category.toStringRes()),
            formattedGroupAmount = formattingHelper.formatCentsWithCurrency(
                expense.groupAmount,
                expense.groupCurrency
            ),
            groupCurrency = expense.groupCurrency,
            formattedSourceAmount = if (isForeignCurrency) {
                formattingHelper.formatCentsWithCurrency(expense.sourceAmount, expense.sourceCurrency)
            } else {
                null
            },
            sourceCurrency = expense.sourceCurrency,
            formattedExchangeRate = if (isForeignCurrency) {
                formattingHelper.formatRateForDisplay(expense.exchangeRate.toPlainString())
            } else {
                null
            },
            isForeignCurrency = isForeignCurrency,
            paymentMethodText = resourceProvider.getString(expense.paymentMethod.toStringRes()),
            paymentMethodIcon = expense.paymentMethod.toIconVector(),
            paymentStatusText = resourceProvider.getString(expense.paymentStatus.toStringRes()),
            paymentStatusIcon = expense.paymentStatus.toIconVector(),
            expenseScopeLabel = buildExpenseScopeLabel(expense.payerType),
            paidByText = paidByText,
            dateText = if (expense.paymentStatus == PaymentStatus.SCHEDULED && expense.dueDate != null) {
                // For scheduled expenses the chip shows the due date, not the creation date.
                formattingHelper.formatShortDate(expense.dueDate)
            } else {
                formattingHelper.formatShortDate(expense.createdAt)
            },
            vendorText = expense.vendor?.takeIf { it.isNotBlank() },
            notesText = expense.notes?.takeIf { it.isNotBlank() },
            scheduledBadgeText = scheduledBadgeText,
            isScheduledPastDue = isScheduledPastDue,
            isOutOfPocket = expense.payerType == PayerType.USER,
            fundingSourceText = buildFundingSourceText(expense, currentUserId, memberProfiles),
            splitTypeText = resourceProvider.getString(expense.splitType.toStringRes()),
            splits = soloSplits,
            splitGroups = splitGroups,
            hasAddOns = expense.addOns.isNotEmpty(),
            hasIncludedAddOns = hasIncludedAddOns,
            addOns = mapAddOns(expense.addOns, expense.groupCurrency),
            formattedEffectiveTotal = effectiveTotal?.let {
                formattingHelper.formatCentsWithCurrency(it, expense.groupCurrency)
            },
            // Only show the decomposed base cost when INCLUDED non-discount add-ons are present
            // (i.e. base-cost extraction actually ran). For INCLUDED-discount-only expenses the
            // groupAmount IS the total paid — labelling it "Base cost" would be misleading.
            formattedIncludedBaseCost = if (hasIncludedNonDiscounts) {
                formattingHelper.formatCentsWithCurrency(expense.groupAmount, expense.groupCurrency)
            } else {
                null
            },
            formattedOriginalEnteredTotal = originalEnteredTotal?.let {
                formattingHelper.formatCentsWithCurrency(it, expense.groupCurrency)
            },
            cashTranches = mapCashTranches(
                expense.cashTranches,
                expense.sourceCurrency,
                expense.groupCurrency,
                withdrawalLookup,
                subunitNameLookup
            ),
            receiptUri = expense.receiptLocalUri,
            createdByText = if (expense.createdBy == currentUserId) {
                resourceProvider.getString(R.string.expense_detail_created_by_you)
            } else {
                resourceProvider.getString(R.string.expense_detail_created_by, paidByName)
            },
            createdAtText = formattingHelper.formatShortDate(expense.createdAt),
            syncStatus = expense.syncStatus
        )
    }

    /**
     * Reconstructs the original user-entered total for expenses that have INCLUDED
     * **non-discount** add-ons.
     *
     * When INCLUDED non-discount add-ons are present [SubmitEventHandler.adjustForIncludedAddOns]
     * extracts the base cost and stores it in [Expense.groupAmount]. The original total the user
     * typed is therefore `base + sum(INCLUDED non-discount amounts)`.
     *
     * INCLUDED DISCOUNT add-ons are deliberately excluded from this sum: those add-ons are
     * informational only — the user already entered the post-discount price — and do **not**
     * participate in base-cost extraction. Mixing them into this reconstruction would yield a
     * value lower than the total paid, which is nonsensical for a discount.
     *
     * This function is only called when [hasIncludedNonDiscounts] is true.
     */
    private fun buildOriginalEnteredTotal(baseGroupAmount: Long, addOns: List<AddOn>): Long {
        val includedNonDiscountTotal = addOns
            .filter { it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT }
            .sumOf { it.groupAmountCents }
        return (baseGroupAmount + includedNonDiscountTotal).coerceAtLeast(0L)
    }

    private fun mapSplits(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        youLabel: String,
        subunitNameLookup: Map<String, String>
    ): Pair<ImmutableList<SplitDetailUiModel>, ImmutableList<SubunitSplitGroupUiModel>> {
        val rows = expense.splits.map { split ->
            split to mapSplitRow(split, expense, memberProfiles, currentUserId, youLabel, subunitNameLookup)
        }
        val solo = rows.filter { it.first.subunitId.isNullOrBlank() }.map { it.second }
        val grouped = rows
            .filter { !it.first.subunitId.isNullOrBlank() }
            .groupBy { it.first.subunitId!! }
            .map { (subunitId, entries) ->
                buildSubunitGroup(subunitId, entries.map { it.second }, expense, subunitNameLookup)
            }
        return solo.toImmutableList() to grouped.toImmutableList()
    }

    private fun buildSubunitGroup(
        subunitId: String,
        members: List<SplitDetailUiModel>,
        expense: Expense,
        subunitNameLookup: Map<String, String>
    ): SubunitSplitGroupUiModel {
        val label = subunitNameLookup[subunitId]
            ?: resourceProvider.getString(R.string.expense_detail_subunit_fallback_label)
        val totalSourceCents = expense.splits
            .filter { it.subunitId == subunitId }
            .sumOf { it.amountCents }
        val totalGroupCents = expenseCalculatorService.computeProportionalAmount(
            amount = totalSourceCents,
            targetAmount = expense.groupAmount,
            totalAmount = expense.sourceAmount
        )
        val isForeignCurrency = expense.sourceCurrency != expense.groupCurrency
        // Fall back to EQUAL for expenses saved before this field was introduced (splitType == null).
        val intraType = expense.splits
            .firstOrNull { it.subunitId == subunitId }
            ?.splitType
            ?: SplitType.EQUAL
        return SubunitSplitGroupUiModel(
            subunitId = subunitId,
            subunitLabel = label,
            formattedTotalAmount = formattingHelper.formatCentsWithCurrency(
                totalGroupCents,
                expense.groupCurrency
            ),
            formattedSourceTotalAmount = if (isForeignCurrency) {
                formattingHelper.formatCentsWithCurrency(totalSourceCents, expense.sourceCurrency)
            } else {
                null
            },
            memberCount = members.size,
            members = members.toImmutableList(),
            splitTypeText = resourceProvider.getString(intraType.toStringRes())
        )
    }

    private fun mapSplitRow(
        split: ExpenseSplit,
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        youLabel: String,
        subunitNameLookup: Map<String, String>
    ): SplitDetailUiModel {
        val groupAmountCents = expenseCalculatorService.computeProportionalAmount(
            amount = split.amountCents,
            targetAmount = expense.groupAmount,
            totalAmount = expense.sourceAmount
        )
        val isForeignCurrency = expense.sourceCurrency != expense.groupCurrency
        val formattedAmount = if (split.isExcluded) {
            resourceProvider.getString(R.string.add_expense_split_member_excluded)
        } else {
            formattingHelper.formatCentsWithCurrency(groupAmountCents, expense.groupCurrency)
        }
        val formattedSourceAmount = if (isForeignCurrency && !split.isExcluded) {
            formattingHelper.formatCentsWithCurrency(split.amountCents, expense.sourceCurrency)
        } else {
            null
        }
        val shareText = split.percentage?.let { pct ->
            "${formattingHelper.formatForDisplay(pct.toPlainString(), 1)}%"
        }
        return SplitDetailUiModel(
            displayName = resolveDisplayName(split.userId, memberProfiles, currentUserId, youLabel),
            formattedAmount = formattedAmount,
            formattedSourceAmount = formattedSourceAmount,
            shareText = shareText,
            isCurrentUser = currentUserId != null && split.userId == currentUserId,
            isExcluded = split.isExcluded,
            subunitId = split.subunitId,
            subunitLabel = split.subunitId?.let { subunitNameLookup[it] }
        )
    }

    private fun mapAddOns(
        addOns: List<AddOn>,
        groupCurrency: String
    ): ImmutableList<AddOnDetailUiModel> = addOns.map { addOn ->
        val isForeign = addOn.currency != groupCurrency
        AddOnDetailUiModel(
            labelText = buildAddOnLabel(addOn),
            modeText = resourceProvider.getString(addOn.mode.toStringRes()),
            formattedAmount = formattingHelper.formatCentsWithCurrency(
                addOn.groupAmountCents,
                groupCurrency
            ),
            formattedSourceAmount = if (isForeign) {
                formattingHelper.formatCentsWithCurrency(addOn.amountCents, addOn.currency)
            } else {
                null
            },
            addOnCurrency = addOn.currency,
            formattedRate = if (isForeign) {
                resourceProvider.getString(
                    R.string.expense_detail_exchange_rate_full,
                    addOn.currency,
                    formattingHelper.formatRateForDisplay(addOn.exchangeRate.toPlainString()),
                    groupCurrency
                )
            } else {
                null
            },
            isForeignCurrency = isForeign,
            isIncluded = addOn.mode == AddOnMode.INCLUDED,
            isDiscount = addOn.type == AddOnType.DISCOUNT
        )
    }.toImmutableList()

    private fun buildAddOnLabel(addOn: AddOn): String {
        val typeName = resourceProvider.getString(addOn.type.toStringRes())
        return if (!addOn.description.isNullOrBlank()) {
            "${addOn.description} ($typeName)"
        } else {
            typeName
        }
    }

    private fun mapCashTranches(
        tranches: List<CashTranche>,
        sourceCurrency: String,
        groupCurrency: String,
        withdrawalLookup: Map<String, CashWithdrawal>,
        subunitNameLookup: Map<String, String>
    ): ImmutableList<CashTrancheDetailUiModel> = tranches.map { tranche ->
        val withdrawal = withdrawalLookup[tranche.withdrawalId]
        val withdrawalTitle = withdrawal?.title
        val label = if (!withdrawalTitle.isNullOrBlank()) {
            withdrawalTitle
        } else {
            val formattedDate = formattingHelper.formatShortDate(withdrawal?.createdAt)
            if (formattedDate.isNotBlank()) {
                resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label, formattedDate)
            } else {
                resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label_no_date)
            }
        }
        val scopeText = resolveTrancheScopeText(withdrawal, subunitNameLookup)
        val formattedRate = buildTrancheRate(withdrawal, groupCurrency)
        CashTrancheDetailUiModel(
            withdrawalLabel = label,
            formattedAmountConsumed = formattingHelper.formatCentsWithCurrency(
                tranche.amountConsumed,
                sourceCurrency
            ),
            scopeText = scopeText,
            formattedRate = formattedRate
        )
    }.toImmutableList()

    private fun buildTrancheRate(withdrawal: CashWithdrawal?, groupCurrency: String): String? {
        if (withdrawal == null) return null
        if (withdrawal.currency == groupCurrency) return null
        if (withdrawal.exchangeRate.compareTo(BigDecimal.ZERO) == 0) return null
        return resourceProvider.getString(
            R.string.expense_detail_exchange_rate_full,
            withdrawal.currency,
            formattingHelper.formatRateForDisplay(withdrawal.exchangeRate.toPlainString()),
            groupCurrency
        )
    }

    private fun resolveTrancheScopeText(
        withdrawal: CashWithdrawal?,
        subunitNameLookup: Map<String, String>
    ): String? {
        if (withdrawal == null) return null
        return when (withdrawal.withdrawalScope) {
            PayerType.GROUP -> resourceProvider.getString(R.string.expense_detail_tranche_scope_group)
            PayerType.USER -> resourceProvider.getString(R.string.expense_detail_tranche_scope_personal)
            PayerType.SUBUNIT -> {
                val name = withdrawal.subunitId?.let { subunitNameLookup[it] }
                if (!name.isNullOrBlank()) {
                    resourceProvider.getString(R.string.expense_detail_tranche_scope_subunit, name)
                } else {
                    null
                }
            }
        }
    }

    private fun buildScheduledBadge(expense: Expense): Pair<String?, Boolean> {
        val dueDate = expense.dueDate
        if (expense.paymentStatus != PaymentStatus.SCHEDULED || dueDate == null) return null to false
        val dueDateLocal = dueDate.toLocalDate()
        return when {
            dueDateLocal.isEqual(LocalDate.now()) ->
                resourceProvider.getString(R.string.expense_scheduled_due_today) to true
            dueDateLocal.isBefore(LocalDate.now()) ->
                resourceProvider.getString(R.string.expense_scheduled_paid) to true
            else ->
                resourceProvider.getString(
                    R.string.expense_scheduled_due_on,
                    formattingHelper.formatShortDate(dueDate)
                ) to false
        }
    }

    private fun buildFundingSourceText(
        expense: Expense,
        currentUserId: String?,
        memberProfiles: Map<String, User>
    ): String? {
        val payerId = expense.payerId ?: expense.createdBy.takeIf { it.isNotBlank() }
        if (expense.payerType != PayerType.USER || payerId == null) return null
        return if (currentUserId != null && payerId == currentUserId) {
            resourceProvider.getString(R.string.expense_paid_by_me)
        } else {
            resourceProvider.getString(
                R.string.expense_paid_by_member,
                resolveDisplayName(payerId, memberProfiles, currentUserId = null, youLabel = "")
            )
        }
    }

    private fun buildExpenseScopeLabel(payerType: PayerType): String = when (payerType) {
        PayerType.GROUP -> resourceProvider.getString(R.string.expense_scope_group)
        PayerType.SUBUNIT -> resourceProvider.getString(R.string.expense_scope_subunit)
        PayerType.USER -> resourceProvider.getString(R.string.expense_scope_personal)
    }

    private fun resolveDisplayName(
        userId: String,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        youLabel: String
    ): String {
        val user = memberProfiles[userId]
        return DisplayNameResolver.resolve(
            userId = userId,
            currentUserId = currentUserId,
            youLabel = youLabel,
            displayName = user?.displayName,
            email = user?.email ?: ""
        )
    }
}
