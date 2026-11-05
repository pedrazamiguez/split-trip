package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.GrammaticalGenderEnum
import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CurrencyBreakdownUiModel
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun mapCurrencyBreakdowns(
    amounts: List<CurrencyAmount>,
    groupCurrency: String,
    locale: Locale
): ImmutableList<CurrencyBreakdownUiModel> {
    return amounts.map { ca ->
        CurrencyBreakdownUiModel(
            currency = ca.currency,
            formattedAmount = formatCurrencyAmount(ca.amountCents, ca.currency, locale),
            formattedEquivalent = if (ca.currency != groupCurrency && ca.equivalentCents > 0) {
                formatCurrencyAmount(ca.equivalentCents, groupCurrency, locale)
            } else {
                ""
            }
        )
    }.toImmutableList()
}

internal fun mapCashBalances(
    balance: GroupPocketBalance,
    locale: Locale
): ImmutableList<CashBalanceUiModel> = balance.cashBalances.entries
    .sortedBy { (currency, _) -> currency }
    .map { (currency, amountCents) ->
        val equivalent = balance.cashEquivalents[currency]
        CashBalanceUiModel(
            currency = currency,
            formattedAmount = formatCurrencyAmount(amountCents, currency, locale),
            formattedEquivalent = if (currency != balance.currency && equivalent != null && equivalent > 0) {
                formatCurrencyAmount(equivalent, balance.currency, locale)
            } else {
                ""
            }
        )
    }.toImmutableList()

internal fun formatIfPos(amount: Long, currency: String, locale: Locale): String? =
    if (amount > 0) formatCurrencyAmount(amount, currency, locale) else null

internal fun resolveMemberDisplay(
    userId: String,
    groupMemberIds: List<String>,
    memberProfiles: Map<String, User>,
    currentUserId: String?,
    userUiMapper: UserUiMapper
): MemberDisplay {
    val resolvedName = userUiMapper.mapToDisplayName(
        user = memberProfiles[userId],
        fallbackUserId = userId,
        currentUserId = currentUserId,
        selfIdentificationContext = if (currentUserId != null) SelfIdentificationContextEnum.NOMINATIVE else null
    )
    return if (userId !in groupMemberIds) {
        MemberDisplay.Former(userId, resolvedName)
    } else {
        MemberDisplay.Active(userId, resolvedName)
    }
}

internal fun resolveCreatedByDisplayName(
    createdBy: String?,
    targetUserId: String,
    memberProfiles: Map<String, User>,
    currentUserId: String?,
    userUiMapper: UserUiMapper
): String? {
    if (createdBy.isNullOrBlank() || createdBy == targetUserId) return null
    if (createdBy == currentUserId) {
        return userUiMapper.mapToSelfIdentification(
            SelfIdentificationContextEnum.PREPOSITIONAL,
            GrammaticalGenderEnum.FEMININE
        )
    }
    return memberProfiles[createdBy]?.displayName
}
