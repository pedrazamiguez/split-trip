package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatSourceAmount
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpenseUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val scheduledBadgeUiMapper: ScheduledBadgeUiMapper
) {

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ExpenseUiModel {
        val appLocale = localeProvider.getCurrentLocale()
        val (badgeText, isPastDue) = scheduledBadgeUiMapper.buildBadge(expense)
        val outOfPocket = expense.payerType == PayerType.USER
        val pairedContribution = pairedContributions[expense.id]
        val effectivePayerId = expense.payerId ?: expense.createdBy.takeIf { it.isNotBlank() }
        val scopeInfo = buildScopeInfo(
            outOfPocket,
            effectivePayerId,
            currentUserId,
            pairedContribution,
            subunits,
            memberProfiles
        )

        return with(expense) {
            val resolvedName = resolveDisplayName(createdBy, memberProfiles)
            ExpenseUiModel(
                id = id,
                title = title,
                formattedAmount = formatAmount(appLocale),
                formattedOriginalAmount = if (sourceCurrency != groupCurrency) {
                    formatSourceAmount(appLocale)
                } else {
                    null
                },
                category = category,
                categoryText = resourceProvider.getString(category.toStringRes()),
                vendorText = vendor,
                paymentMethodText = resourceProvider.getString(paymentMethod.toStringRes()),
                paymentStatusText = resourceProvider.getString(paymentStatus.toStringRes()),
                paidByText = resourceProvider.getString(R.string.paid_by, resolvedName),
                dateText = createdAt?.formatShortDate(appLocale) ?: "",
                scheduledBadgeText = badgeText,
                isScheduledPastDue = isPastDue,
                hasAddOns = addOns.isNotEmpty(),
                isOutOfPocket = outOfPocket,
                fundingSourceText = scopeInfo.text,
                isSubunitScope = scopeInfo.isSubunit,
                isGroupScope = scopeInfo.isGroup,
                syncStatus = syncStatus
            )
        }
    }

    fun mapList(
        expenses: List<Expense>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ImmutableList<ExpenseUiModel> =
        expenses.map { map(it, memberProfiles, currentUserId, pairedContributions, subunits) }.toImmutableList()

    /**
     * Groups expenses by date (from createdAt) and produces date headers
     * with the formatted daily total in the group's default currency.
     *
     * Expenses are already sorted DESC by createdAt from the DAO.
     * The groupCurrencyCode is taken from the first expense in the list.
     */
    fun mapGroupedByDate(
        expenses: List<Expense>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ImmutableList<ExpenseDateGroupUiModel> {
        if (expenses.isEmpty()) return emptyList<ExpenseDateGroupUiModel>().toImmutableList()

        val appLocale = localeProvider.getCurrentLocale()
        val groupCurrencyCode = expenses.first().groupCurrency

        return expenses
            .groupBy { it.createdAt?.toLocalDate() }
            .map { (date, dayExpenses) ->
                val dateText = date?.let {
                    LocalDateTime.of(it, LocalTime.MIDNIGHT)
                        .formatShortDate(appLocale)
                } ?: ""

                val dayTotalCents = dayExpenses.sumOf { it.groupAmount }
                val formattedDayTotal = formatCurrencyAmount(
                    amount = dayTotalCents,
                    currencyCode = groupCurrencyCode,
                    locale = appLocale
                )

                ExpenseDateGroupUiModel(
                    dateText = dateText,
                    formattedDayTotal = formattedDayTotal,
                    expenses = dayExpenses.map {
                        map(it, memberProfiles, currentUserId, pairedContributions, subunits)
                    }.toImmutableList()
                )
            }
            .toImmutableList()
    }

    /**
     * Builds scope-aware funding source info for out-of-pocket expenses.
     *
     * Uses the paired contribution's scope when available to determine:
     * - The display text (e.g., "Paid by me", "Paid for Cantalobos")
     * - Whether the scope is SUBUNIT or GROUP (for icon selection)
     *
     * Falls back to simple payer name when no paired contribution exists.
     */
    @Suppress("LongParameterList")
    private fun buildScopeInfo(
        isOutOfPocket: Boolean,
        payerId: String?,
        currentUserId: String?,
        pairedContribution: Contribution?,
        subunits: Map<String, Subunit>,
        memberProfiles: Map<String, User>
    ): ScopeInfo {
        if (!isOutOfPocket || payerId == null) return ScopeInfo.EMPTY

        val isCurrentUser = currentUserId != null && payerId == currentUserId
        val scope = pairedContribution?.contributionScope ?: PayerType.USER

        val text = when (scope) {
            PayerType.SUBUNIT -> buildSubunitScopeText(
                isCurrentUser,
                payerId,
                pairedContribution,
                subunits,
                memberProfiles
            )
            PayerType.GROUP -> buildGroupScopeText(isCurrentUser, payerId, memberProfiles)
            else -> buildPersonalScopeText(isCurrentUser, payerId, memberProfiles)
        }

        return ScopeInfo(
            text = text,
            isSubunit = scope == PayerType.SUBUNIT,
            isGroup = scope == PayerType.GROUP
        )
    }

    private fun buildPersonalScopeText(
        isCurrentUser: Boolean,
        payerId: String,
        memberProfiles: Map<String, User>
    ): String = if (isCurrentUser) {
        resourceProvider.getString(R.string.expense_paid_by_me)
    } else {
        resourceProvider.getString(R.string.expense_paid_by_member, resolveDisplayName(payerId, memberProfiles))
    }

    private fun buildSubunitScopeText(
        isCurrentUser: Boolean,
        payerId: String,
        pairedContribution: Contribution?,
        subunits: Map<String, Subunit>,
        memberProfiles: Map<String, User>
    ): String {
        val subunitName = pairedContribution?.subunitId?.let { subunits[it]?.name }
            ?: return buildPersonalScopeText(isCurrentUser, payerId, memberProfiles)

        return if (isCurrentUser) {
            resourceProvider.getString(R.string.expense_paid_for_scope, subunitName)
        } else {
            val payerName = resolveDisplayName(payerId, memberProfiles)
            resourceProvider.getString(R.string.expense_paid_by_member_for_scope, payerName, subunitName)
        }
    }

    private fun buildGroupScopeText(
        isCurrentUser: Boolean,
        payerId: String,
        memberProfiles: Map<String, User>
    ): String {
        val everyoneLabel = resourceProvider.getString(R.string.expense_scope_everyone)
        return if (isCurrentUser) {
            resourceProvider.getString(R.string.expense_paid_for_scope, everyoneLabel)
        } else {
            val payerName = resolveDisplayName(payerId, memberProfiles)
            resourceProvider.getString(R.string.expense_paid_by_member_for_scope, payerName, everyoneLabel)
        }
    }

    /**
     * Resolves a userId to a human-readable display name using the
     * fallback hierarchy: displayName → email → raw userId.
     */
    private fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>): String {
        val user = memberProfiles[userId] ?: return userId
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email.takeIf { it.isNotBlank() }
            ?: userId
    }

    /**
     * Internal data holder for scope-aware funding source info.
     */
    private data class ScopeInfo(
        val text: String?,
        val isSubunit: Boolean,
        val isGroup: Boolean
    ) {
        companion object {
            val EMPTY = ScopeInfo(text = null, isSubunit = false, isGroup = false)
        }
    }
}
