package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatSourceAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentBadgeData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpenseUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val paymentStatusBadgeUiMapper: PaymentStatusBadgeUiMapper,
    private val userUiMapper: UserUiMapper
) {

    fun map(
        expense: Expense,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap(),
        groupMemberIds: List<String> = emptyList()
    ): ExpenseUiModel {
        val appLocale = localeProvider.getCurrentLocale()
        val badge = resolveBadge(expense)
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
            val resolvedName = resolveDisplayName(createdBy, memberProfiles, currentUserId)
            val creatorDisplay = resolveCreatorDisplay(createdBy, resolvedName, groupMemberIds)
            ExpenseUiModel(
                id = id,
                title = title,
                formattedAmount = formatAmount(appLocale),
                formattedOriginalAmount = if (sourceCurrency != groupCurrency) formatSourceAmount(appLocale) else null,
                category = category,
                categoryText = resourceProvider.getString(category.toStringRes()),
                subcategory = subcategory,
                subcategoryText = resolveSubcategoryText(subcategory),
                vendorText = vendor,
                paymentMethodText = resourceProvider.getString(paymentMethod.toStringRes()),
                paymentStatusText = resourceProvider.getString(paymentStatus.toStringRes()),
                paidByText = resourceProvider.getString(R.string.paid_by, resolvedName),
                creatorDisplay = creatorDisplay,
                dateText = effectiveDate?.formatShortDate(appLocale) ?: "",
                badgeText = badge.text,
                badgeIcon = badge.icon,
                isBadgeUrgent = badge.isUrgent,
                hasAddOns = addOns.isNotEmpty(),
                isOutOfPocket = outOfPocket,
                fundingSourceText = scopeInfo.text,
                isSubunitScope = scopeInfo.isSubunit,
                isGroupScope = scopeInfo.isGroup,
                syncStatus = syncStatus,
                isCancelled = expense.paymentStatus == PaymentStatus.CANCELLED,
                isRefundable = expense.paymentStatus == PaymentStatus.REFUNDABLE && !badge.isUrgent,
                isComposite = isComposite,
                subExpenseCount = subExpenses.size,
                paidPercentage = paidPercentage.toInt()
            )
        }
    }

    fun mapList(
        expenses: List<Expense>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap(),
        groupMemberIds: List<String> = emptyList()
    ): ImmutableList<ExpenseUiModel> =
        expenses.map {
            map(it, memberProfiles, currentUserId, pairedContributions, subunits, groupMemberIds)
        }.toImmutableList()

    /**
     * Groups expenses by date (from effectiveDate) and produces date headers
     * with the formatted daily total in the group's default currency.
     *
     * Expenses are already sorted DESC by effectiveDate from the DAO.
     * The groupCurrencyCode is taken from the first expense in the list.
     */
    fun mapGroupedByDate(
        expenses: List<Expense>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null,
        pairedContributions: Map<String, Contribution> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap(),
        groupMemberIds: List<String> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): ImmutableList<ExpenseDateGroupUiModel> {
        if (expenses.isEmpty()) return emptyList<ExpenseDateGroupUiModel>().toImmutableList()

        val appLocale = localeProvider.getCurrentLocale()
        val groupCurrencyCode = expenses.first().groupCurrency

        return expenses
            .groupBy { it.effectiveDate?.toLocalDate() }
            .map { (date, dayExpenses) ->
                val dateText = date?.let {
                    LocalDateTime.of(it, LocalTime.MIDNIGHT)
                        .formatShortDate(appLocale)
                } ?: ""

                val dayTotalCents = dayExpenses
                    .filter { it.paymentStatus != PaymentStatus.CANCELLED }
                    .filterNot {
                        it.paymentStatus == PaymentStatus.SCHEDULED &&
                            it.dueDate?.toLocalDate()?.isAfter(today) == true
                    }
                    .sumOf { it.groupAmount }

                val dayScheduledCents = dayExpenses
                    .filter { it.paymentStatus != PaymentStatus.CANCELLED }
                    .filter {
                        it.paymentStatus == PaymentStatus.SCHEDULED &&
                            it.dueDate?.toLocalDate()?.isAfter(today) == true
                    }
                    .sumOf { it.groupAmount }

                val formattedDayTotal = formatCurrencyAmount(
                    amount = dayTotalCents,
                    currencyCode = groupCurrencyCode,
                    locale = appLocale
                )

                val formattedDayScheduled = if (dayScheduledCents > 0L) {
                    formatCurrencyAmount(
                        amount = dayScheduledCents,
                        currencyCode = groupCurrencyCode,
                        locale = appLocale
                    )
                } else {
                    null
                }

                ExpenseDateGroupUiModel(
                    dateText = dateText,
                    formattedDayTotal = formattedDayTotal,
                    formattedDayScheduled = formattedDayScheduled,
                    expenses = dayExpenses.map {
                        map(it, memberProfiles, currentUserId, pairedContributions, subunits, groupMemberIds)
                    }.toImmutableList()
                )
            }
            .toImmutableList()
    }

    /**
     * Formats the total spent amount for visible/filtered expenses in the group's default currency.
     */
    fun formatTotalSpent(
        totalSpentCents: Long,
        currencyCode: String
    ): String = formatCurrencyAmount(
        amount = totalSpentCents,
        currencyCode = currencyCode,
        locale = localeProvider.getCurrentLocale()
    )

    /**
     * Formats the total pending scheduled amount for visible/filtered expenses in the group's default currency.
     */
    fun formatScheduledAmount(
        totalScheduledCents: Long,
        currencyCode: String
    ): String = formatCurrencyAmount(
        amount = totalScheduledCents,
        currencyCode = currencyCode,
        locale = localeProvider.getCurrentLocale()
    )

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
                currentUserId,
                pairedContribution,
                subunits,
                memberProfiles
            )
            PayerType.GROUP -> buildGroupScopeText(isCurrentUser, payerId, currentUserId, memberProfiles)
            else -> buildPersonalScopeText(isCurrentUser, payerId, currentUserId, memberProfiles)
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
        @Suppress("UNUSED_PARAMETER") currentUserId: String?,
        memberProfiles: Map<String, User>
    ): String = if (isCurrentUser) {
        resourceProvider.getString(R.string.expense_paid_by_me)
    } else {
        resourceProvider.getString(
            R.string.expense_paid_by_member,
            resolveDisplayName(payerId, memberProfiles, currentUserId)
        )
    }

    private fun buildSubunitScopeText(
        isCurrentUser: Boolean,
        payerId: String,
        currentUserId: String?,
        pairedContribution: Contribution?,
        subunits: Map<String, Subunit>,
        memberProfiles: Map<String, User>
    ): String {
        val subunitName = pairedContribution?.subunitId?.let { subunits[it]?.name }
            ?: return buildPersonalScopeText(isCurrentUser, payerId, currentUserId, memberProfiles)

        return if (isCurrentUser) {
            resourceProvider.getString(R.string.expense_paid_for_scope, subunitName)
        } else {
            val payerName = resolveDisplayName(payerId, memberProfiles, currentUserId)
            resourceProvider.getString(R.string.expense_paid_by_member_for_scope, payerName, subunitName)
        }
    }

    private fun buildGroupScopeText(
        isCurrentUser: Boolean,
        payerId: String,
        currentUserId: String?,
        memberProfiles: Map<String, User>
    ): String {
        val everyoneLabel = resourceProvider.getString(R.string.expense_scope_everyone)
        return if (isCurrentUser) {
            resourceProvider.getString(R.string.expense_paid_for_scope, everyoneLabel)
        } else {
            val payerName = resolveDisplayName(payerId, memberProfiles, currentUserId)
            resourceProvider.getString(R.string.expense_paid_by_member_for_scope, payerName, everyoneLabel)
        }
    }

    private fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>, currentUserId: String?): String {
        return userUiMapper.mapToDisplayName(
            user = memberProfiles[userId],
            fallbackUserId = userId,
            currentUserId = currentUserId,
            selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
        )
    }

    private fun resolveBadge(expense: Expense): BadgeResult {
        return when {
            expense.paymentStatus == PaymentStatus.PARTIAL -> {
                BadgeResult(
                    text = resourceProvider.getString(
                        R.string.expense_badge_partial,
                        expense.paidPercentage.toInt()
                    ),
                    icon = TablerIcons.Outline.Clock,
                    isUrgent = false
                )
            }
            expense.paymentStatus == PaymentStatus.FINISHED && expense.subExpenses.size >= 2 -> {
                BadgeResult(
                    text = resourceProvider.getQuantityString(
                        R.plurals.expense_badge_payments,
                        expense.subExpenses.size,
                        expense.subExpenses.size
                    ),
                    icon = TablerIcons.Outline.CircleCheck,
                    isUrgent = false
                )
            }
            else -> {
                val badgeData = paymentStatusBadgeUiMapper.buildBadge(expense)
                BadgeResult(
                    text = badgeData?.text,
                    icon = resolveBadgeIcon(expense.paymentStatus, badgeData),
                    isUrgent = badgeData?.isPassed == true
                )
            }
        }
    }

    private data class BadgeResult(
        val text: String?,
        val icon: ImageVector?,
        val isUrgent: Boolean
    )

    private fun resolveBadgeIcon(paymentStatus: PaymentStatus, badgeData: PaymentBadgeData?): ImageVector? {
        if (badgeData == null) return null
        return when (paymentStatus) {
            PaymentStatus.SCHEDULED -> if (badgeData.isPassed) {
                TablerIcons.Outline.CircleCheck
            } else {
                TablerIcons.Outline.Calendar
            }
            PaymentStatus.REFUNDABLE -> if (badgeData.isPassed) {
                TablerIcons.Outline.Receipt
            } else {
                TablerIcons.Outline.ReceiptRefund
            }
            else -> null
        }
    }

    private fun resolveCreatorDisplay(
        createdBy: String,
        resolvedName: String,
        groupMemberIds: List<String>
    ): MemberDisplay = if (createdBy !in groupMemberIds) {
        MemberDisplay.Former(createdBy, resolvedName)
    } else {
        MemberDisplay.Active(createdBy, resolvedName)
    }

    private fun resolveSubcategoryText(subcategory: ExpenseSubcategory): String? =
        if (subcategory != ExpenseSubcategory.UNSPECIFIED) {
            resourceProvider.getString(subcategory.toStringRes())
        } else {
            null
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
