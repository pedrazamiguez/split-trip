package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
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
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpenseDetailUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val addOnCalculationService: AddOnCalculationService,
    private val paymentStatusBadgeUiMapper: PaymentStatusBadgeUiMapper,
    private val userUiMapper: UserUiMapper,
    private val subExpenseDetailUiMapper: SubExpenseDetailUiMapper
) {

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        withdrawalLookup: Map<String, CashWithdrawal> = emptyMap(),
        subunitNameLookup: Map<String, String> = emptyMap(),
        groupMemberIds: List<String> = emptyList()
    ): ExpenseDetailUiModel {
        return ModelBuilder(
            expense,
            memberProfiles,
            currentUserId,
            withdrawalLookup,
            subunitNameLookup,
            groupMemberIds
        ).build()
    }

    private inner class ModelBuilder(
        val expense: Expense,
        val memberProfiles: Map<String, User>,
        val currentUserId: String?,
        val withdrawalLookup: Map<String, CashWithdrawal>,
        val subunitNameLookup: Map<String, String>,
        val groupMemberIds: List<String>
    ) {
        @Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
        fun build(): ExpenseDetailUiModel {
            val youLabel = resourceProvider.getString(R.string.you_label)
            val (soloSplits, splitGroups) = resolveSplits()
            val isForeign = expense.sourceCurrency != expense.groupCurrency
            val badgeData = paymentStatusBadgeUiMapper.buildBadge(expense)
            val badgeIcon = badgeData?.let {
                when (expense.paymentStatus) {
                    PaymentStatus.SCHEDULED -> if (it.isPassed) {
                        TablerIcons.Outline.CircleCheck
                    } else {
                        TablerIcons.Outline.Calendar
                    }
                    PaymentStatus.REFUNDABLE -> if (it.isPassed) {
                        TablerIcons.Outline.Receipt
                    } else {
                        TablerIcons.Outline.ReceiptRefund
                    }
                    else -> null
                }
            }

            val effectivePayerId = expense.payerId ?: expense.createdBy.takeIf { it.isNotBlank() }
            val payerDisplay = resolvePayerDisplay(effectivePayerId, youLabel)
            val creatorDisplay = resolveCreatorDisplay(youLabel)

            val isScheduled = expense.paymentStatus == PaymentStatus.SCHEDULED
            val (formattedExpectedGroupAmount, formattedGroupAmountDifference) =
                buildScheduledShiftInfo()
            val placeholder = formattingHelper.formatCentsWithCurrency(0L, expense.groupCurrency)

            return ExpenseDetailUiModel(
                id = expense.id,
                groupId = expense.groupId,
                title = expense.title,
                category = expense.category,
                categoryText = resourceProvider.getString(expense.category.toStringRes()),
                subcategory = expense.subcategory,
                subcategoryText = if (expense.subcategory != ExpenseSubcategory.UNSPECIFIED) {
                    resourceProvider.getString(expense.subcategory.toStringRes())
                } else {
                    null
                },
                formattedGroupAmount = formattingHelper.formatCentsWithCurrency(
                    expense.groupAmount,
                    expense.groupCurrency
                ),
                groupCurrency = expense.groupCurrency,
                formattedSourceAmount = resolveSourceAmountFormatted(expense, formattingHelper, isForeign),
                sourceCurrency = expense.sourceCurrency,
                formattedExchangeRate = resolveExchangeRateFormatted(expense, formattingHelper, isForeign),
                isForeignCurrency = isForeign,
                isScheduled = isScheduled,
                formattedExpectedGroupAmount = formattedExpectedGroupAmount,
                formattedGroupAmountDifference = formattedGroupAmountDifference,
                formattedConfirmPaymentPlaceholder = placeholder,
                paymentMethodText = resourceProvider.getString(expense.paymentMethod.toStringRes()),
                paymentMethodIcon = expense.paymentMethod.toIconVector(),
                paymentStatusText = when (expense.paymentStatus) {
                    PaymentStatus.SCHEDULED -> resourceProvider.getString(R.string.payment_status_pending)
                    PaymentStatus.REFUNDABLE -> resourceProvider.getString(R.string.expense_detail_on_hold)
                    else -> resourceProvider.getString(expense.paymentStatus.toStringRes())
                },
                paymentStatusIcon = expense.paymentStatus.toIconVector(),
                expenseScopeLabel = buildExpenseScopeLabel(expense.payerType, resourceProvider),
                paidByText = getPaidByText(
                    expense,
                    currentUserId,
                    youLabel,
                    memberProfiles,
                    userUiMapper,
                    resourceProvider
                ),
                payerDisplay = payerDisplay,
                creatorDisplay = creatorDisplay,
                dateText = formattingHelper.formatShortDate(expense.effectiveDate),
                secondaryDateText = resolveSecondaryDateText(expense, formattingHelper),
                secondaryDateIcon = resolveSecondaryDateIcon(expense),
                vendorText = expense.vendor?.takeIf { it.isNotBlank() },
                notesText = expense.notes?.takeIf { it.isNotBlank() },
                badgeText = null,
                badgeIcon = badgeIcon,
                isBadgeUrgent = badgeData?.isPassed == true,
                isOutOfPocket = expense.payerType == PayerType.USER,
                fundingSourceText = buildFundingSourceText(
                    expense,
                    currentUserId,
                    memberProfiles,
                    resourceProvider,
                    userUiMapper
                ),
                splitTypeText = resourceProvider.getString(expense.splitType.toStringRes()),
                splits = soloSplits,
                splitGroups = splitGroups,
                hasAddOns = expense.addOns.isNotEmpty(),
                hasIncludedAddOns = expense.addOns.any { it.mode == AddOnMode.INCLUDED },
                addOns = mapAddOns(expense.addOns, expense.groupCurrency),
                formattedEffectiveTotal = formatEffectiveTotal(expense, formattingHelper, addOnCalculationService),
                formattedIncludedBaseCost = formatIncludedBaseCost(expense, formattingHelper),
                formattedOriginalEnteredTotal = formatOriginalEnteredTotal(expense, formattingHelper),
                cashTranches = mapCashTranches(
                    expense.cashTranches,
                    expense.sourceCurrency,
                    expense.groupCurrency,
                    withdrawalLookup,
                    subunitNameLookup
                ),
                receiptUri = expense.receiptAttachment?.let { it.localUri.ifBlank { it.remoteUrl } },
                receiptMimeType = expense.receiptAttachment?.mimeType,
                createdByText = getCreatedByText(
                    expense,
                    currentUserId,
                    youLabel,
                    memberProfiles,
                    userUiMapper,
                    resourceProvider
                ),
                createdAtText = formattingHelper.formatShortDate(expense.createdAt),
                syncStatus = expense.syncStatus,
                isCancelled = expense.paymentStatus == PaymentStatus.CANCELLED,
                isRefundable = expense.paymentStatus == PaymentStatus.REFUNDABLE && badgeData?.isPassed != true,
                isComposite = expense.isComposite,
                subExpenses = subExpenseDetailUiMapper.mapList(
                    expense.subExpenses,
                    expense.groupCurrency,
                    memberProfiles,
                    currentUserId
                ),
                paidPercentage = expense.paidPercentage.toInt()
            )
        }

        /**
         * Calculates formatted expected amount and difference string for expenses
         * where the actual charge shifted from the originally expected group amount.
         * Returns (formattedExpectedGroupAmount, formattedGroupAmountDifference),
         * both null when no shift occurred.
         */
        private fun buildScheduledShiftInfo(): Pair<String?, String?> {
            val hasShift = expense.paymentStatus == PaymentStatus.FINISHED &&
                expense.expectedGroupAmount != null &&
                expense.expectedGroupAmount != expense.groupAmount
            if (!hasShift) return null to null

            val formattedExpected = formattingHelper.formatCentsWithCurrency(
                expense.expectedGroupAmount!!,
                expense.groupCurrency
            )
            val shift = expense.groupAmount - expense.expectedGroupAmount!!
            val formattedShiftValue = formattingHelper.formatCentsWithCurrency(
                abs(shift),
                expense.groupCurrency
            )
            val formattedDifference = if (shift >= 0) "+$formattedShiftValue" else "-$formattedShiftValue"
            return formattedExpected to formattedDifference
        }

        private fun resolvePayerDisplay(effectivePayerId: String?, youLabel: String): MemberDisplay {
            val payerResolvedName = if (effectivePayerId != null) {
                resolveDisplayName(effectivePayerId, memberProfiles, currentUserId, youLabel, userUiMapper)
            } else {
                ""
            }
            return if (effectivePayerId == null || effectivePayerId !in groupMemberIds) {
                MemberDisplay.Former(effectivePayerId ?: "", payerResolvedName)
            } else {
                MemberDisplay.Active(effectivePayerId, payerResolvedName)
            }
        }

        private fun resolveCreatorDisplay(youLabel: String): MemberDisplay {
            val creatorResolvedName =
                resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel, userUiMapper)
            return if (expense.createdBy.isBlank() || expense.createdBy !in groupMemberIds) {
                MemberDisplay.Former(expense.createdBy, creatorResolvedName)
            } else {
                MemberDisplay.Active(expense.createdBy, creatorResolvedName)
            }
        }

        private fun resolveSplits() = mapSplits(
            expense = expense,
            memberProfiles = memberProfiles,
            currentUserId = currentUserId,
            subunitNameLookup = subunitNameLookup,
            groupMemberIds = groupMemberIds
        )
    }

    private fun mapSplits(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        subunitNameLookup: Map<String, String>,
        groupMemberIds: List<String>
    ): Pair<ImmutableList<SplitDetailUiModel>, ImmutableList<SubunitSplitGroupUiModel>> {
        val rows = expense.splits.map { split ->
            split to mapSplitRow(
                split = split,
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                subunitNameLookup = subunitNameLookup,
                groupMemberIds = groupMemberIds
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
        subunitNameLookup: Map<String, String>,
        groupMemberIds: List<String>
    ): SplitDetailUiModel {
        val youLabel = resourceProvider.getString(R.string.you_label)
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
        val resolvedName = resolveDisplayName(split.userId, memberProfiles, currentUserId, youLabel, userUiMapper)
        val memberDisplay = if (split.userId !in groupMemberIds) {
            MemberDisplay.Former(split.userId, resolvedName)
        } else {
            MemberDisplay.Active(split.userId, resolvedName)
        }
        return SplitDetailUiModel(
            displayName = resolvedName,
            formattedAmount = formattedAmount,
            formattedSourceAmount = formattedSourceAmount,
            shareText = shareText,
            isCurrentUser = currentUserId != null && split.userId == currentUserId,
            isExcluded = split.isExcluded,
            subunitId = split.subunitId,
            subunitLabel = split.subunitId?.let { subunitNameLookup[it] },
            memberDisplay = memberDisplay
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

private fun resolveSourceAmountFormatted(
    expense: Expense,
    formattingHelper: FormattingHelper,
    isForeign: Boolean
): String? {
    return if (isForeign) {
        formattingHelper.formatCentsWithCurrency(expense.sourceAmount, expense.sourceCurrency)
    } else {
        null
    }
}

private fun resolveExchangeRateFormatted(
    expense: Expense,
    formattingHelper: FormattingHelper,
    isForeign: Boolean
): String? {
    return if (isForeign) {
        formattingHelper.formatRateForDisplay(
            expense.exchangeRate.toPlainString()
        )
    } else {
        null
    }
}

private fun formatIncludedBaseCost(expense: Expense, formattingHelper: FormattingHelper): String? {
    val hasIncludedNonDiscounts = expense.addOns.any {
        it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
    }
    return if (hasIncludedNonDiscounts) {
        formattingHelper.formatCentsWithCurrency(
            expense.groupAmount,
            expense.groupCurrency
        )
    } else {
        null
    }
}

private fun formatOriginalEnteredTotal(expense: Expense, formattingHelper: FormattingHelper): String? {
    val hasIncludedNonDiscounts = expense.addOns.any {
        it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
    }
    return if (hasIncludedNonDiscounts) {
        formattingHelper.formatCentsWithCurrency(
            buildOriginalEnteredTotal(expense.groupAmount, expense.addOns),
            expense.groupCurrency
        )
    } else {
        null
    }
}

private fun formatEffectiveTotal(
    expense: Expense,
    formattingHelper: FormattingHelper,
    addOnCalculationService: AddOnCalculationService
): String? {
    return resolveEffectiveTotal(expense.groupAmount, expense.addOns, addOnCalculationService)?.let {
        formattingHelper.formatCentsWithCurrency(it, expense.groupCurrency)
    }
}

private fun getPaidByText(
    expense: Expense,
    currentUserId: String?,
    youLabel: String,
    memberProfiles: Map<String, User>,
    userUiMapper: UserUiMapper,
    resourceProvider: ResourceProvider
): String {
    val paidByName = resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel, userUiMapper)
    return resolvePaidByText(expense.createdBy, currentUserId, paidByName, resourceProvider)
}

private fun getCreatedByText(
    expense: Expense,
    currentUserId: String?,
    youLabel: String,
    memberProfiles: Map<String, User>,
    userUiMapper: UserUiMapper,
    resourceProvider: ResourceProvider
): String {
    val paidByName = resolveDisplayName(expense.createdBy, memberProfiles, currentUserId, youLabel, userUiMapper)
    return resolveCreatedByText(expense.createdBy, currentUserId, paidByName, resourceProvider)
}
