package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentBadgeData
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseDetailUiModel
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class SubExpenseDetailUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) {

    fun map(
        subExpense: SubExpense,
        groupCurrency: String,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null
    ): SubExpenseDetailUiModel {
        val locale = localeProvider.getCurrentLocale()
        val formattedAmount = formatCurrencyAmount(
            amount = subExpense.amountCents,
            currencyCode = subExpense.currency,
            locale = locale
        )
        val formattedGroupAmount = if (subExpense.currency != groupCurrency) {
            formatCurrencyAmount(
                amount = subExpense.groupAmountCents,
                currencyCode = groupCurrency,
                locale = locale
            )
        } else {
            null
        }

        val payerText = resolvePayerText(subExpense, memberProfiles, currentUserId)
        val date = subExpense.operationDate ?: subExpense.dueDate
        val dateText = date?.formatShortDate(locale) ?: ""

        val badgeData = buildBadge(subExpense)
        val badgeIcon = resolveBadgeIcon(subExpense.paymentStatus, badgeData?.isPassed == true)
        val formattedEffectiveTotal = resolveEffectiveTotal(subExpense, groupCurrency, locale)

        return SubExpenseDetailUiModel(
            id = subExpense.id,
            title = subExpense.title,
            formattedAmount = formattedAmount,
            formattedGroupAmount = formattedGroupAmount,
            paymentMethodText = resourceProvider.getString(subExpense.paymentMethod.toStringRes()),
            paymentMethodIcon = subExpense.paymentMethod.toIconVector(),
            paymentStatus = subExpense.paymentStatus,
            paymentStatusText = resourceProvider.getString(subExpense.paymentStatus.toStringRes()),
            paymentStatusIcon = subExpense.paymentStatus.toIconVector(),
            badgeText = badgeData?.text,
            badgeIcon = badgeIcon,
            isBadgeUrgent = badgeData?.isPassed == true,
            payerText = payerText,
            dateText = dateText,
            notesText = subExpense.notes,
            hasAddOns = subExpense.addOns.isNotEmpty(),
            formattedEffectiveTotal = formattedEffectiveTotal
        )
    }

    fun mapList(
        subExpenses: List<SubExpense>,
        groupCurrency: String,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null
    ): ImmutableList<SubExpenseDetailUiModel> =
        subExpenses.map {
            map(it, groupCurrency, memberProfiles, currentUserId)
        }.toImmutableList()

    private fun buildBadge(subExpense: SubExpense): PaymentBadgeData? {
        val dueDate = subExpense.dueDate ?: return null
        if (subExpense.paymentStatus != PaymentStatus.SCHEDULED) return null

        val today = LocalDate.now()
        val dueDateLocal = dueDate.toLocalDate()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        return when {
            dueDateLocal.isBefore(yesterday) ->
                PaymentBadgeData(
                    text = dueDate.formatShortDate(localeProvider.getCurrentLocale()),
                    isPassed = true,
                    isToday = false
                )
            dueDateLocal.isEqual(yesterday) ->
                PaymentBadgeData(
                    text = resourceProvider.getString(R.string.expense_relative_yesterday),
                    isPassed = true,
                    isToday = false
                )
            dueDateLocal.isEqual(today) ->
                PaymentBadgeData(
                    text = resourceProvider.getString(R.string.expense_relative_today),
                    isPassed = false,
                    isToday = true
                )
            dueDateLocal.isEqual(tomorrow) ->
                PaymentBadgeData(
                    text = resourceProvider.getString(R.string.expense_relative_tomorrow),
                    isPassed = false,
                    isToday = false
                )
            else ->
                PaymentBadgeData(
                    text = dueDate.formatShortDate(localeProvider.getCurrentLocale()),
                    isPassed = false,
                    isToday = false
                )
        }
    }

    private fun resolvePayerText(
        subExpense: SubExpense,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): String? {
        val payerId = subExpense.payerId
        return if (subExpense.payerType == PayerType.USER && payerId != null) {
            val resolvedName = userUiMapper.mapToDisplayName(
                user = memberProfiles[payerId],
                fallbackUserId = payerId,
                currentUserId = currentUserId,
                selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
            )
            resourceProvider.getString(R.string.paid_by, resolvedName)
        } else {
            null
        }
    }

    private fun resolveBadgeIcon(paymentStatus: PaymentStatus, isPassed: Boolean) = when (paymentStatus) {
        PaymentStatus.SCHEDULED -> if (isPassed) {
            TablerIcons.Outline.CircleCheck
        } else {
            TablerIcons.Outline.Calendar
        }
        PaymentStatus.PARTIAL -> TablerIcons.Outline.Clock
        else -> null
    }

    private fun resolveEffectiveTotal(
        subExpense: SubExpense,
        groupCurrency: String,
        locale: java.util.Locale
    ): String? {
        if (subExpense.addOns.isEmpty()) return null
        val total = subExpense.groupAmountCents + subExpense.addOns.sumOf { it.groupAmountCents }
        return formatCurrencyAmount(
            amount = total,
            currencyCode = groupCurrency,
            locale = locale
        )
    }
}
