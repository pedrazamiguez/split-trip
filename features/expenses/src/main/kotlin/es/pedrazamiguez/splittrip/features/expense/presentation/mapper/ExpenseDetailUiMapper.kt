package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpenseDetailUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val addOnCalculationService: AddOnCalculationService,
    private val scheduledBadgeUiMapper: ScheduledBadgeUiMapper,
    private val userUiMapper: UserUiMapper
) {

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        withdrawalLookup: Map<String, CashWithdrawal> = emptyMap(),
        subunitNameLookup: Map<String, String> = emptyMap()
    ): ExpenseDetailUiModel {
        return ModelBuilder(expense, memberProfiles, currentUserId, withdrawalLookup, subunitNameLookup).build()
    }

    private inner class ModelBuilder(
        val expense: Expense,
        val memberProfiles: Map<String, User>,
        val currentUserId: String?,
        val withdrawalLookup: Map<String, CashWithdrawal>,
        val subunitNameLookup: Map<String, String>
    ) {
        fun build(): ExpenseDetailUiModel {
            val youLabel = resourceProvider.getString(R.string.you_label)
            val (soloSplits, splitGroups) = resolveSplits(youLabel)
            val isForeign = expense.sourceCurrency != expense.groupCurrency
            val (scheduledBadgeText, isScheduledPastDue) = scheduledBadgeUiMapper.buildBadge(expense)

            return ExpenseDetailUiModel(
                id = expense.id,
                groupId = expense.groupId,
                title = expense.title,
                category = expense.category,
                categoryText = resourceProvider.getString(expense.category.toStringRes()),
                formattedGroupAmount = formatGroupAmount(),
                groupCurrency = expense.groupCurrency,
                formattedSourceAmount = resolveSourceAmountFormatted(isForeign),
                sourceCurrency = expense.sourceCurrency,
                formattedExchangeRate = resolveExchangeRateFormatted(isForeign),
                isForeignCurrency = isForeign,
                paymentMethodText = resourceProvider.getString(expense.paymentMethod.toStringRes()),
                paymentMethodIcon = expense.paymentMethod.toIconVector(),
                paymentStatusText = resourceProvider.getString(expense.paymentStatus.toStringRes()),
                paymentStatusIcon = expense.paymentStatus.toIconVector(),
                expenseScopeLabel = buildExpenseScopeLabel(expense.payerType, resourceProvider),
                paidByText = getPaidByText(youLabel),
                dateText = resolveDateText(expense, formattingHelper),
                vendorText = expense.vendor?.takeIf { it.isNotBlank() },
                notesText = expense.notes?.takeIf { it.isNotBlank() },
                scheduledBadgeText = scheduledBadgeText,
                isScheduledPastDue = isScheduledPastDue,
                isOutOfPocket = expense.payerType == PayerType.USER,
                fundingSourceText = resolveFundingSourceText(),
                splitTypeText = resourceProvider.getString(expense.splitType.toStringRes()),
                splits = soloSplits,
                splitGroups = splitGroups,
                hasAddOns = expense.addOns.isNotEmpty(),
                hasIncludedAddOns = expense.addOns.any { it.mode == AddOnMode.INCLUDED },
                addOns = mapAddOns(expense.addOns, expense.groupCurrency),
                formattedEffectiveTotal = formatEffectiveTotal(),
                formattedIncludedBaseCost = formatIncludedBaseCost(),
                formattedOriginalEnteredTotal = formatOriginalEnteredTotal(),
                cashTranches = resolveCashTranches(),
                receiptUri = expense.receiptAttachment?.let { it.localUri.ifBlank { it.remoteUrl } },
                receiptMimeType = expense.receiptAttachment?.mimeType,
                createdByText = getCreatedByText(youLabel),
                createdAtText = formattingHelper.formatShortDate(expense.createdAt),
                syncStatus = expense.syncStatus,
                isCancelled = expense.paymentStatus == PaymentStatus.CANCELLED,
                isRefundable = expense.paymentStatus == PaymentStatus.REFUNDABLE
            )
        }

        private fun resolveSourceAmountFormatted(isForeign: Boolean): String? {
            return if (isForeign) {
                formattingHelper.formatCentsWithCurrency(expense.sourceAmount, expense.sourceCurrency)
            } else {
                null
            }
        }

        private fun resolveExchangeRateFormatted(isForeign: Boolean): String? {
            return if (isForeign) {
                formattingHelper.formatRateForDisplay(
                    expense.exchangeRate.toPlainString()
                )
            } else {
                null
            }
        }

        private fun resolveSplits(youLabel: String) = mapSplits(
            expense,
            memberProfiles,
            currentUserId,
            youLabel,
            subunitNameLookup,
            userUiMapper
        )

        private fun resolveFundingSourceText() = buildFundingSourceText(
            expense = expense,
            currentUserId = currentUserId,
            memberProfiles = memberProfiles,
            resourceProvider = resourceProvider,
            userUiMapper = userUiMapper
        )

        private fun resolveCashTranches() = mapCashTranches(
            expense.cashTranches,
            expense.sourceCurrency,
            expense.groupCurrency,
            withdrawalLookup,
            subunitNameLookup
        )

        private fun formatGroupAmount() = formattingHelper.formatCentsWithCurrency(
            expense.groupAmount,
            expense.groupCurrency
        )
        private fun getPaidByText(
            youLabel: String
        ) = resolvePaidByText(
            expense.createdBy,
            currentUserId,
            resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel, userUiMapper),
            resourceProvider
        )
        private fun formatEffectiveTotal() = resolveEffectiveTotal(
            expense.groupAmount,
            expense.addOns,
            addOnCalculationService
        )?.let {
            formattingHelper.formatCentsWithCurrency(it, expense.groupCurrency)
        }
        private fun formatIncludedBaseCost() = if (hasIncludedNonDiscounts()) {
            formattingHelper.formatCentsWithCurrency(
                expense.groupAmount,
                expense.groupCurrency
            )
        } else {
            null
        }
        private fun formatOriginalEnteredTotal() = if (hasIncludedNonDiscounts()) {
            formattingHelper.formatCentsWithCurrency(
                buildOriginalEnteredTotal(expense.groupAmount, expense.addOns),
                expense.groupCurrency
            )
        } else {
            null
        }
        private fun getCreatedByText(
            youLabel: String
        ) = resolveCreatedByText(
            expense.createdBy,
            currentUserId,
            resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel, userUiMapper),
            resourceProvider
        )
        private fun hasIncludedNonDiscounts() = expense.addOns.any {
            it.mode == AddOnMode.INCLUDED &&
                it.type != AddOnType.DISCOUNT
        }
    }

    private fun mapSplits(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        youLabel: String,
        subunitNameLookup: Map<String, String>,
        userUiMapper: UserUiMapper
    ): Pair<ImmutableList<SplitDetailUiModel>, ImmutableList<SubunitSplitGroupUiModel>> {
        val rows = expense.splits.map { split ->
            split to mapSplitRow(
                split = split,
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                youLabel = youLabel,
                subunitNameLookup = subunitNameLookup,
                userUiMapper = userUiMapper
            )
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
        subunitNameLookup: Map<String, String>,
        userUiMapper: UserUiMapper
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
            displayName = resolveDisplayName(split.userId, memberProfiles, currentUserId, youLabel, userUiMapper),
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
            labelText = buildAddOnLabel(addOn, resourceProvider),
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
        val scopeText = resolveTrancheScopeText(withdrawal, subunitNameLookup, resourceProvider)
        val formattedRate = buildTrancheRate(withdrawal, groupCurrency, formattingHelper, resourceProvider)
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
}

private fun buildOriginalEnteredTotal(baseGroupAmount: Long, addOns: List<AddOn>): Long {
    val includedNonDiscountTotal = addOns
        .filter { it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT }
        .sumOf { it.groupAmountCents }
    return (baseGroupAmount + includedNonDiscountTotal).coerceAtLeast(0L)
}

private fun resolveDisplayName(
    userId: String,
    memberProfiles: Map<String, User>,
    currentUserId: String?,
    youLabel: String,
    userUiMapper: UserUiMapper
): String {
    val user = memberProfiles[userId]
    return userUiMapper.mapToDisplayName(
        user = user,
        fallbackUserId = userId,
        currentUserId = currentUserId,
        youLabel = youLabel
    )
}

private fun resolvePaidByText(
    createdBy: String,
    currentUserId: String?,
    paidByName: String,
    resourceProvider: ResourceProvider
): String {
    return if (createdBy == currentUserId) {
        resourceProvider.getString(R.string.paid_by_you)
    } else {
        resourceProvider.getString(R.string.paid_by, paidByName)
    }
}

private fun resolveEffectiveTotal(
    groupAmount: Long,
    addOns: List<AddOn>,
    addOnCalculationService: AddOnCalculationService
): Long? {
    return if (addOns.isNotEmpty()) {
        addOnCalculationService.calculateEffectiveGroupAmount(groupAmount, addOns)
    } else {
        null
    }
}

private fun resolveDateText(expense: Expense, formattingHelper: FormattingHelper): String {
    return if (expense.paymentStatus == PaymentStatus.SCHEDULED && expense.dueDate != null) {
        formattingHelper.formatShortDate(expense.dueDate)
    } else {
        formattingHelper.formatShortDate(expense.createdAt)
    }
}

private fun resolveCreatedByText(
    createdBy: String,
    currentUserId: String?,
    paidByName: String,
    resourceProvider: ResourceProvider
): String {
    return if (createdBy == currentUserId) {
        resourceProvider.getString(R.string.expense_detail_created_by_you)
    } else {
        resourceProvider.getString(R.string.expense_detail_created_by, paidByName)
    }
}

private fun buildAddOnLabel(addOn: AddOn, resourceProvider: ResourceProvider): String {
    val typeName = resourceProvider.getString(addOn.type.toStringRes())
    return if (!addOn.description.isNullOrBlank()) {
        "${addOn.description} ($typeName)"
    } else {
        typeName
    }
}

private fun buildTrancheRate(
    withdrawal: CashWithdrawal?,
    groupCurrency: String,
    formattingHelper: FormattingHelper,
    resourceProvider: ResourceProvider
): String? {
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
    subunitNameLookup: Map<String, String>,
    resourceProvider: ResourceProvider
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

private fun buildFundingSourceText(
    expense: Expense,
    currentUserId: String?,
    memberProfiles: Map<String, User>,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): String? {
    val payerId = expense.payerId ?: expense.createdBy.takeIf { it.isNotBlank() }
    if (expense.payerType != PayerType.USER || payerId == null) return null
    return if (currentUserId != null && payerId == currentUserId) {
        resourceProvider.getString(R.string.expense_paid_by_me)
    } else {
        resourceProvider.getString(
            R.string.expense_paid_by_member,
            resolveDisplayName(payerId, memberProfiles, currentUserId = null, youLabel = "", userUiMapper)
        )
    }
}

private fun buildExpenseScopeLabel(payerType: PayerType, resourceProvider: ResourceProvider): String =
    when (payerType) {
        PayerType.GROUP -> resourceProvider.getString(R.string.expense_scope_group)
        PayerType.SUBUNIT -> resourceProvider.getString(R.string.expense_scope_subunit)
        PayerType.USER -> resourceProvider.getString(R.string.expense_scope_personal)
    }
