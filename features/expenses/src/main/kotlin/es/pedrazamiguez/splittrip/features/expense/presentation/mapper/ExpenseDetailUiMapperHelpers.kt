package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.BreakdownSummaryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ReconstructedAmounts
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Collator
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun buildOriginalEnteredTotal(baseGroupAmount: Long, addOns: List<AddOn>): Long {
    val includedNonDiscountTotal = addOns
        .filter { it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT }
        .sumOf { it.groupAmountCents }
    return (baseGroupAmount + includedNonDiscountTotal).coerceAtLeast(0L)
}

internal fun resolveAddOnGroupAmountCents(addOn: AddOn): Long {
    if (addOn.groupAmountCents > 0L) return addOn.groupAmountCents
    if (addOn.amountCents > 0L && addOn.type == AddOnType.DISCOUNT) {
        return BigDecimal(addOn.amountCents)
            .multiply(addOn.exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
    return addOn.groupAmountCents
}

internal fun resolveIncludedBaseCostFormatted(
    expense: Expense,
    formattingHelper: FormattingHelper
): String? {
    val totalResolvedDiscountsCents = expense.addOns
        .filter { it.type == AddOnType.DISCOUNT }
        .sumOf { resolveAddOnGroupAmountCents(it) }
    val hasIncludedNonDiscounts = expense.addOns.any {
        it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
    }
    return if (totalResolvedDiscountsCents > 0L || hasIncludedNonDiscounts) {
        val baseCostCents = expense.groupAmount + totalResolvedDiscountsCents
        formattingHelper.formatCentsWithCurrency(
            baseCostCents,
            expense.groupCurrency
        )
    } else {
        null
    }
}

internal fun resolveOriginalEnteredTotalFormatted(
    expense: Expense,
    formattingHelper: FormattingHelper
): String? {
    val hasIncludedNonDiscounts = expense.addOns.any {
        it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
    }
    val hasNoOnTop = expense.addOns.none { it.mode == AddOnMode.ON_TOP }
    return if (hasIncludedNonDiscounts && hasNoOnTop) {
        formattingHelper.formatCentsWithCurrency(
            buildOriginalEnteredTotal(expense.groupAmount, expense.addOns),
            expense.groupCurrency
        )
    } else {
        null
    }
}

internal fun resolvePreDiscountAmounts(expense: Expense): ReconstructedAmounts {
    val onTopDiscounts = expense.addOns.filter {
        it.type == AddOnType.DISCOUNT && it.mode == AddOnMode.ON_TOP
    }
    if (onTopDiscounts.isEmpty()) {
        return ReconstructedAmounts(
            sourceAmount = expense.sourceAmount,
            groupAmount = expense.groupAmount
        )
    }

    val totalDiscountGroupCents = onTopDiscounts.sumOf { resolveAddOnGroupAmountCents(it) }
    val preDiscountGroupAmount = expense.groupAmount + totalDiscountGroupCents
    val preDiscountSourceAmount = when {
        onTopDiscounts.all { it.currency == expense.sourceCurrency } -> {
            expense.sourceAmount + onTopDiscounts.sumOf { it.amountCents }
        }
        expense.groupAmount > 0L -> {
            BigDecimal(expense.sourceAmount)
                .multiply(BigDecimal(preDiscountGroupAmount))
                .divide(BigDecimal(expense.groupAmount), 0, RoundingMode.HALF_UP)
                .toLong()
        }
        expense.exchangeRate.compareTo(BigDecimal.ZERO) > 0 -> {
            BigDecimal(preDiscountGroupAmount)
                .divide(expense.exchangeRate, 0, RoundingMode.HALF_UP)
                .toLong()
        }
        else -> preDiscountGroupAmount
    }
    return ReconstructedAmounts(
        sourceAmount = preDiscountSourceAmount,
        groupAmount = preDiscountGroupAmount
    )
}

internal fun resolveEffectiveSourceAmount(expense: Expense): Long {
    val nonDiscountSourceExtras = expense.addOns
        .filter { it.currency == expense.sourceCurrency && it.type != AddOnType.DISCOUNT }
        .sumOf { it.amountCents }
    return expense.sourceAmount + nonDiscountSourceExtras
}

internal fun resolvePreExtractionAmounts(
    expense: Expense,
    baseAmounts: ReconstructedAmounts
): ReconstructedAmounts {
    val includedNonDiscounts = expense.addOns.filter {
        it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
    }
    if (includedNonDiscounts.isEmpty()) return baseAmounts

    val totalIncludedGroupCents = includedNonDiscounts.sumOf { resolveAddOnGroupAmountCents(it) }
    val enteredGroupAmount = baseAmounts.groupAmount + totalIncludedGroupCents

    val enteredSourceAmount = when {
        includedNonDiscounts.all { it.currency == expense.sourceCurrency } -> {
            baseAmounts.sourceAmount + includedNonDiscounts.sumOf { it.amountCents }
        }
        baseAmounts.groupAmount > 0L -> {
            BigDecimal(baseAmounts.sourceAmount)
                .multiply(BigDecimal(enteredGroupAmount))
                .divide(BigDecimal(baseAmounts.groupAmount), 0, RoundingMode.HALF_UP)
                .toLong()
        }
        expense.exchangeRate.compareTo(BigDecimal.ZERO) > 0 -> {
            BigDecimal(enteredGroupAmount)
                .divide(expense.exchangeRate, 0, RoundingMode.HALF_UP)
                .toLong()
        }
        else -> enteredGroupAmount
    }
    return ReconstructedAmounts(
        sourceAmount = enteredSourceAmount,
        groupAmount = enteredGroupAmount
    )
}

internal fun resolveBreakdownSummary(
    addOns: List<AddOn>,
    formattedEffectiveTotal: String?,
    formattedOriginalEnteredTotal: String?
): BreakdownSummaryUiModel? {
    val hasOnTop = addOns.any { it.mode == AddOnMode.ON_TOP }
    return when {
        !hasOnTop && formattedOriginalEnteredTotal != null -> {
            BreakdownSummaryUiModel(
                labelRes = R.string.expense_detail_breakdown_original_total,
                formattedAmount = formattedOriginalEnteredTotal
            )
        }
        formattedEffectiveTotal != null -> {
            BreakdownSummaryUiModel(
                labelRes = R.string.expense_review_effective_total,
                formattedAmount = formattedEffectiveTotal
            )
        }
        formattedOriginalEnteredTotal != null -> {
            BreakdownSummaryUiModel(
                labelRes = R.string.expense_review_effective_total,
                formattedAmount = formattedOriginalEnteredTotal
            )
        }
        else -> null
    }
}

internal fun resolveDisplayName(
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

internal fun resolvePaidByText(
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

internal fun resolveEffectiveTotal(
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

internal fun resolveSecondaryDateText(expense: Expense, formattingHelper: FormattingHelper): String? {
    return when (expense.paymentStatus) {
        PaymentStatus.SCHEDULED, PaymentStatus.REFUNDABLE -> expense.dueDate?.let {
            formattingHelper.formatShortDate(it)
        }
        else -> null
    }
}

internal fun resolveSecondaryDateIcon(expense: Expense): ImageVector? {
    return when (expense.paymentStatus) {
        PaymentStatus.SCHEDULED -> TablerIcons.Outline.Calendar
        PaymentStatus.REFUNDABLE -> TablerIcons.Outline.ReceiptRefund
        else -> null
    }
}

internal fun resolveCreatedByText(
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

internal fun buildAddOnLabel(addOn: AddOn, resourceProvider: ResourceProvider): String {
    val typeName = resourceProvider.getString(addOn.type.toStringRes())
    return if (!addOn.description.isNullOrBlank()) {
        "${addOn.description} ($typeName)"
    } else {
        typeName
    }
}

internal fun buildTrancheRate(
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

internal fun resolveTrancheScopeText(
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

internal fun buildFundingSourceText(
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

internal fun buildExpenseScopeLabel(payerType: PayerType, resourceProvider: ResourceProvider): String =
    when (payerType) {
        PayerType.GROUP -> resourceProvider.getString(R.string.expense_scope_group)
        PayerType.SUBUNIT -> resourceProvider.getString(R.string.expense_scope_subunit)
        PayerType.USER -> resourceProvider.getString(R.string.expense_scope_personal)
    }

internal fun getExpenseDetailEntityPriority(
    isUserEntity: Boolean,
    isUserInSubunit: Boolean,
    isSubunit: Boolean
): Int {
    if (isUserEntity) return 0
    return if (isUserInSubunit) {
        if (isSubunit) 1 else 2
    } else {
        if (!isSubunit) 1 else 2
    }
}

internal fun sortSplitBreakdownItems(
    items: List<SplitBreakdownItemUiModel>,
    currentUserId: String?,
    locale: Locale
): ImmutableList<SplitBreakdownItemUiModel> {
    val collator = Collator.getInstance(locale)
    val isUserInSubunit = currentUserId != null &&
        items.any { item ->
            item is SplitBreakdownItemUiModel.Subunit && item.group.members.any { it.isCurrentUser }
        }

    return items.sortedWith { itemA, itemB ->
        val isUserEntityA = when (itemA) {
            is SplitBreakdownItemUiModel.Solo -> itemA.split.isCurrentUser
            is SplitBreakdownItemUiModel.Subunit -> itemA.group.members.any { it.isCurrentUser }
        }
        val isSubunitA = itemA is SplitBreakdownItemUiModel.Subunit
        val priorityA = getExpenseDetailEntityPriority(
            isUserEntity = isUserEntityA,
            isUserInSubunit = isUserInSubunit,
            isSubunit = isSubunitA
        )

        val isUserEntityB = when (itemB) {
            is SplitBreakdownItemUiModel.Solo -> itemB.split.isCurrentUser
            is SplitBreakdownItemUiModel.Subunit -> itemB.group.members.any { it.isCurrentUser }
        }
        val isSubunitB = itemB is SplitBreakdownItemUiModel.Subunit
        val priorityB = getExpenseDetailEntityPriority(
            isUserEntity = isUserEntityB,
            isUserInSubunit = isUserInSubunit,
            isSubunit = isSubunitB
        )

        if (priorityA != priorityB) {
            priorityA.compareTo(priorityB)
        } else {
            val nameA = when (itemA) {
                is SplitBreakdownItemUiModel.Solo -> itemA.split.displayName
                is SplitBreakdownItemUiModel.Subunit -> itemA.group.subunitLabel
            }
            val nameB = when (itemB) {
                is SplitBreakdownItemUiModel.Solo -> itemB.split.displayName
                is SplitBreakdownItemUiModel.Subunit -> itemB.group.subunitLabel
            }
            collator.compare(nameA, nameB)
        }
    }.toImmutableList()
}

internal fun sortSubunitMembers(
    members: List<SplitDetailUiModel>,
    currentUserId: String?,
    locale: Locale
): ImmutableList<SplitDetailUiModel> {
    val collator = Collator.getInstance(locale)
    return members.sortedWith(
        compareBy<SplitDetailUiModel> {
            val isUser = it.isCurrentUser || (currentUserId != null && it.memberDisplay.userId == currentUserId)
            if (isUser) 0 else 1
        }.thenComparator { a, b ->
            collator.compare(a.displayName, b.displayName)
        }
    ).toImmutableList()
}
