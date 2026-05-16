package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTrancheDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
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
        val paidByName = resolveDisplayName(expense.createdBy, memberProfiles)

        val effectiveTotal = if (expense.addOns.isNotEmpty()) {
            addOnCalculationService.calculateEffectiveGroupAmount(expense.groupAmount, expense.addOns)
        } else {
            null
        }

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
            paymentStatusText = resourceProvider.getString(expense.paymentStatus.toStringRes()),
            paidByText = resourceProvider.getString(R.string.paid_by, paidByName),
            dateText = formattingHelper.formatShortDate(expense.createdAt),
            vendorText = expense.vendor?.takeIf { it.isNotBlank() },
            notesText = expense.notes?.takeIf { it.isNotBlank() },
            scheduledBadgeText = scheduledBadgeText,
            isScheduledPastDue = isScheduledPastDue,
            isOutOfPocket = expense.payerType == PayerType.USER,
            fundingSourceText = buildFundingSourceText(expense, currentUserId, memberProfiles),
            splitTypeText = resourceProvider.getString(expense.splitType.toStringRes()),
            splits = mapSplits(expense, memberProfiles, currentUserId),
            hasAddOns = expense.addOns.isNotEmpty(),
            addOns = mapAddOns(expense.addOns, expense.groupCurrency),
            formattedEffectiveTotal = effectiveTotal?.let {
                formattingHelper.formatCentsWithCurrency(it, expense.groupCurrency)
            },
            cashTranches = mapCashTranches(
                expense.cashTranches,
                expense.sourceCurrency,
                withdrawalLookup,
                subunitNameLookup
            ),
            receiptUri = expense.receiptLocalUri,
            createdByText = paidByName,
            createdAtText = formattingHelper.formatShortDate(expense.createdAt),
            syncStatus = expense.syncStatus
        )
    }

    private fun mapSplits(
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<SplitDetailUiModel> = expense.splits.map { split ->
        mapSplitRow(split, expense, memberProfiles, currentUserId)
    }.toImmutableList()

    private fun mapSplitRow(
        split: ExpenseSplit,
        expense: Expense,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): SplitDetailUiModel {
        // Convert split amount from source currency to group currency for display
        val groupAmountCents = expenseCalculatorService.computeProportionalAmount(
            amount = split.amountCents,
            targetAmount = expense.groupAmount,
            totalAmount = expense.sourceAmount
        )
        val formattedAmount = if (split.isExcluded) {
            resourceProvider.getString(R.string.add_expense_split_member_excluded)
        } else {
            formattingHelper.formatCentsWithCurrency(groupAmountCents, expense.groupCurrency)
        }
        val shareText = split.percentage?.let { pct ->
            "${formattingHelper.formatForDisplay(pct.toPlainString(), 1)}%"
        }
        return SplitDetailUiModel(
            displayName = resolveDisplayName(split.userId, memberProfiles),
            formattedAmount = formattedAmount,
            shareText = shareText,
            isCurrentUser = currentUserId != null && split.userId == currentUserId,
            isExcluded = split.isExcluded
        )
    }

    private fun mapAddOns(
        addOns: List<AddOn>,
        groupCurrency: String
    ): ImmutableList<AddOnDetailUiModel> = addOns.map { addOn ->
        AddOnDetailUiModel(
            labelText = buildAddOnLabel(addOn),
            modeText = resourceProvider.getString(addOn.mode.toStringRes()),
            formattedAmount = formattingHelper.formatCentsWithCurrency(
                addOn.groupAmountCents,
                groupCurrency
            ),
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
        CashTrancheDetailUiModel(
            withdrawalLabel = label,
            formattedAmountConsumed = formattingHelper.formatCentsWithCurrency(
                tranche.amountConsumed,
                sourceCurrency
            ),
            scopeText = scopeText
        )
    }.toImmutableList()

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
                resolveDisplayName(payerId, memberProfiles)
            )
        }
    }

    private fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>): String {
        val user = memberProfiles[userId] ?: return userId
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email.takeIf { it.isNotBlank() }
            ?: userId
    }
}
