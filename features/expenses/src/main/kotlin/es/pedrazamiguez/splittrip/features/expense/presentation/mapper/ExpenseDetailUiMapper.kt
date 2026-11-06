package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.utcToSystemLocalDateTime
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
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
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toDetailStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTrancheDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel
import java.time.ZoneId
import java.util.Locale
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
    private val localeProvider: LocaleProvider? = null
) {

    private data class SplitBreakdownResult(
        val soloSplits: ImmutableList<SplitDetailUiModel>,
        val splitGroups: ImmutableList<SubunitSplitGroupUiModel>,
        val splitBreakdownItems: ImmutableList<SplitBreakdownItemUiModel>
    )

    private data class MappedSplit(
        val split: ExpenseSplit,
        val detail: SplitDetailUiModel
    )

    private data class ScheduledShiftInfo(
        val formattedExpectedGroupAmount: String?,
        val formattedGroupAmountDifference: String?
    )

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        withdrawalLookup: Map<String, CashWithdrawal> = emptyMap(),
        subunitNameLookup: Map<String, String> = emptyMap(),
        groupMemberIds: List<String> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ExpenseDetailUiModel {
        return ModelBuilder(
            expense,
            memberProfiles,
            currentUserId,
            withdrawalLookup,
            subunitNameLookup,
            groupMemberIds,
            zoneId
        ).build()
    }

    private inner class ModelBuilder(
        val expense: Expense,
        val memberProfiles: Map<String, User>,
        val currentUserId: String?,
        val withdrawalLookup: Map<String, CashWithdrawal>,
        val subunitNameLookup: Map<String, String>,
        val groupMemberIds: List<String>,
        val zoneId: ZoneId
    ) {
        private val effectiveGroupAmount: Long = resolveEffectiveTotal(
            expense.groupAmount,
            expense.addOns,
            addOnCalculationService
        ) ?: expense.groupAmount

        @Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
        fun build(): ExpenseDetailUiModel {
            val youLabel = resourceProvider.getString(R.string.you_label)
            val (soloSplits, splitGroups, splitBreakdownItems) = resolveSplits()
            val isForeign = expense.sourceCurrency != expense.groupCurrency
            val badgeData = paymentStatusBadgeUiMapper.buildBadge(expense)
            val isScheduledSettled = badgeData?.isPassed == true || badgeData?.isToday == true
            val badgeIcon = badgeData?.let {
                when (expense.paymentStatus) {
                    PaymentStatus.SCHEDULED -> if (isScheduledSettled) {
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
            val scheduledShiftInfo = buildScheduledShiftInfo()
            val placeholder = formattingHelper.formatCentsWithCurrency(0L, expense.groupCurrency)

            val formattedEffectiveTotal = formatEffectiveTotal(expense, formattingHelper, addOnCalculationService)
            val formattedIncludedBaseCost = formatIncludedBaseCost(expense, formattingHelper)
            val formattedOriginalEnteredTotal = formatOriginalEnteredTotal(expense, formattingHelper)
            val breakdownSummary = resolveBreakdownSummary(
                addOns = expense.addOns,
                formattedEffectiveTotal = formattedEffectiveTotal,
                formattedOriginalEnteredTotal = formattedOriginalEnteredTotal
            )

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
                formattedExpectedGroupAmount = scheduledShiftInfo.formattedExpectedGroupAmount,
                formattedGroupAmountDifference = scheduledShiftInfo.formattedGroupAmountDifference,
                formattedConfirmPaymentPlaceholder = placeholder,
                paymentMethodText = resourceProvider.getString(expense.paymentMethod.toStringRes()),
                paymentMethodIcon = expense.paymentMethod.toIconVector(),
                paymentStatusText = when (expense.paymentStatus) {
                    PaymentStatus.SCHEDULED -> if (isScheduledSettled) {
                        resourceProvider.getString(R.string.payment_status_finished)
                    } else {
                        resourceProvider.getString(R.string.payment_status_pending)
                    }
                    PaymentStatus.REFUNDABLE -> if (badgeData?.isPassed == true) {
                        resourceProvider.getString(R.string.payment_status_finished)
                    } else {
                        resourceProvider.getString(R.string.expense_detail_on_hold)
                    }
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
                dateText = formattingHelper.formatShortDate(expense.effectiveDate?.utcToSystemLocalDateTime(zoneId)),
                secondaryDateText = resolveSecondaryDateText(expense, formattingHelper),
                secondaryDateIcon = resolveSecondaryDateIcon(expense),
                formattedRefundedDate = resolveRefundedDateText(expense, formattingHelper, resourceProvider, zoneId),
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
                splitBreakdownItems = splitBreakdownItems,
                hasAddOns = expense.addOns.isNotEmpty(),
                hasIncludedAddOns = expense.addOns.any { it.mode == AddOnMode.INCLUDED },
                addOns = mapAddOns(expense.addOns, expense.groupCurrency),
                breakdownSummary = breakdownSummary,
                formattedEffectiveTotal = formattedEffectiveTotal,
                formattedIncludedBaseCost = formattedIncludedBaseCost,
                formattedOriginalEnteredTotal = formattedOriginalEnteredTotal,
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
                createdAtText = formattingHelper.formatShortDate(expense.createdAt?.utcToSystemLocalDateTime(zoneId)),
                syncStatus = expense.syncStatus,
                isCancelled = expense.paymentStatus == PaymentStatus.CANCELLED,
                isRefundable = expense.paymentStatus == PaymentStatus.REFUNDABLE && badgeData?.isPassed != true
            )
        }

        private fun buildScheduledShiftInfo(): ScheduledShiftInfo {
            val hasShift = expense.paymentStatus == PaymentStatus.FINISHED &&
                expense.expectedGroupAmount != null &&
                expense.expectedGroupAmount != expense.groupAmount
            if (!hasShift) {
                return ScheduledShiftInfo(
                    formattedExpectedGroupAmount = null,
                    formattedGroupAmountDifference = null
                )
            }

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
            return ScheduledShiftInfo(
                formattedExpectedGroupAmount = formattedExpected,
                formattedGroupAmountDifference = formattedDifference
            )
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

        private fun resolveSplits(): SplitBreakdownResult {
            val locale = localeProvider?.getCurrentLocale() ?: Locale.getDefault()
            val mappedSplits = expense.splits.map { split ->
                MappedSplit(
                    split = split,
                    detail = mapSplitRow(split)
                )
            }
            val solo = mappedSplits
                .filter { it.split.subunitId.isNullOrBlank() }
                .map { it.detail }
            val grouped = mappedSplits
                .filter { !it.split.subunitId.isNullOrBlank() }
                .groupBy { it.split.subunitId!! }
                .map { (subunitId, entries) ->
                    buildSubunitGroup(
                        subunitId = subunitId,
                        members = entries.map { it.detail },
                        locale = locale
                    )
                }
            val unsortedItems = solo.map { SplitBreakdownItemUiModel.Solo(it) } +
                grouped.map { SplitBreakdownItemUiModel.Subunit(it) }
            val sortedBreakdownItems = sortSplitBreakdownItems(unsortedItems, currentUserId, locale)
            val sortedSolo = sortedBreakdownItems.filterIsInstance<SplitBreakdownItemUiModel.Solo>()
                .map { it.split }
                .toImmutableList()
            val sortedGrouped = sortedBreakdownItems.filterIsInstance<SplitBreakdownItemUiModel.Subunit>()
                .map { it.group }
                .toImmutableList()
            return SplitBreakdownResult(sortedSolo, sortedGrouped, sortedBreakdownItems)
        }

        private fun buildSubunitGroup(
            subunitId: String,
            members: List<SplitDetailUiModel>,
            locale: Locale
        ): SubunitSplitGroupUiModel {
            val label = subunitNameLookup[subunitId]
                ?: resourceProvider.getString(R.string.expense_detail_subunit_fallback_label)
            val totalSourceCents = expense.splits
                .filter { it.subunitId == subunitId }
                .sumOf { it.amountCents }
            val totalGroupCents = expenseCalculatorService.computeProportionalAmount(
                amount = totalSourceCents,
                targetAmount = effectiveGroupAmount,
                totalAmount = expense.sourceAmount
            )
            val isForeignCurrency = expense.sourceCurrency != expense.groupCurrency
            val intraType = expense.splits
                .firstOrNull { it.subunitId == subunitId }
                ?.splitType
                ?: SplitType.EQUAL
            val sortedMembers = sortSubunitMembers(members, currentUserId, locale)
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
                memberCount = sortedMembers.size,
                members = sortedMembers,
                splitTypeText = resourceProvider.getString(intraType.toStringRes())
            )
        }

        private fun mapSplitRow(split: ExpenseSplit): SplitDetailUiModel {
            val youLabel = resourceProvider.getString(R.string.you_label)
            val groupAmountCents = expenseCalculatorService.computeProportionalAmount(
                amount = split.amountCents,
                targetAmount = effectiveGroupAmount,
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
    }

    private fun mapAddOns(
        addOns: List<AddOn>,
        groupCurrency: String
    ): ImmutableList<AddOnDetailUiModel> = addOns.map { addOn ->
        val isForeign = addOn.currency != groupCurrency
        val resolvedGroupCents = resolveAddOnGroupAmountCents(addOn)
        AddOnDetailUiModel(
            labelText = buildAddOnLabel(addOn, resourceProvider),
            modeText = resourceProvider.getString(addOn.mode.toDetailStringRes()),
            formattedAmount = formattingHelper.formatCentsWithCurrency(
                resolvedGroupCents,
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

private fun formatIncludedBaseCost(expense: Expense, formattingHelper: FormattingHelper): String? =
    resolveIncludedBaseCostFormatted(expense, formattingHelper)

private fun formatOriginalEnteredTotal(expense: Expense, formattingHelper: FormattingHelper): String? =
    resolveOriginalEnteredTotalFormatted(expense, formattingHelper)

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
