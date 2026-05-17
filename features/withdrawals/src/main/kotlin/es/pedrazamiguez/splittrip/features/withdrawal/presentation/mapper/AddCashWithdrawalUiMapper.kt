package es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.common.util.DisplayNameResolver
import es.pedrazamiguez.splittrip.core.designsystem.extension.resolveLocalizedName
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.withdrawal.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class AddCashWithdrawalUiMapper(
    private val resourceProvider: ResourceProvider
) {

    // ── Domain → UI Model Mapping ──────────────────────────────────────────

    fun mapCurrency(currency: Currency): CurrencyUiModel = CurrencyUiModel(
        code = currency.code,
        displayText = currency.formatDisplay(),
        decimalDigits = currency.decimalDigits,
        defaultName = currency.defaultName,
        localizedName = currency.resolveLocalizedName(resourceProvider)
    )

    fun mapCurrencies(currencies: List<Currency>): ImmutableList<CurrencyUiModel> = currencies.map {
        mapCurrency(it)
    }.toImmutableList()

    // ── Member Mapping ──────────────────────────────────────────────────────

    /**
     * Maps a list of member user IDs and their profiles to [MemberOptionUiModel] items
     * for display in the member picker.
     *
     * @param memberIds     Group member user IDs.
     * @param memberProfiles Resolved profiles keyed by userId.
     * @param currentUserId The authenticated user's ID (used to set [MemberOptionUiModel.isCurrentUser]).
     */
    fun toMemberOptions(
        memberIds: List<String>,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<MemberOptionUiModel> = memberIds.map { memberId ->
        MemberOptionUiModel(
            userId = memberId,
            displayName = memberProfiles[memberId]?.displayName?.takeIf { it.isNotBlank() }
                ?: memberId,
            isCurrentUser = memberId == currentUserId
        )
    }.toImmutableList()

    /**
     * Looks up the display name for a given userId from a pre-mapped member list.
     *
     * Returns [youLabel] when the member has [MemberOptionUiModel.isCurrentUser] set to `true`,
     * delegating "you" personalisation to [DisplayNameResolver].
     *
     * @return The member's display name or [youLabel] for the current user; `""` if not found.
     */
    fun resolveDisplayName(
        userId: String?,
        members: ImmutableList<MemberOptionUiModel>,
        youLabel: String = ""
    ): String {
        if (userId == null) return ""
        val member = members.firstOrNull { it.userId == userId } ?: return ""
        // When no youLabel is provided, skip "you" personalisation and return the display
        // name directly — avoids blank labels at call sites that don't supply the string.
        if (youLabel.isBlank()) return member.displayName
        return DisplayNameResolver.resolve(
            userId = userId,
            currentUserId = if (member.isCurrentUser) userId else null,
            youLabel = youLabel,
            displayName = member.displayName
        )
    }

    // ── Label Building ─────────────────────────────────────────────────────

    fun buildExchangeRateLabel(groupCurrency: CurrencyUiModel, selectedCurrency: CurrencyUiModel): String =
        resourceProvider.getString(
            R.string.withdrawal_rate_label_format,
            groupCurrency.displayText,
            selectedCurrency.displayText
        )

    fun buildDeductedAmountLabel(groupCurrency: CurrencyUiModel): String = resourceProvider.getString(
        R.string.withdrawal_deducted_label_format,
        groupCurrency.displayText
    )

    fun buildFeeConvertedLabel(groupCurrency: CurrencyUiModel): String = resourceProvider.getString(
        R.string.withdrawal_fee_converted_hint,
        groupCurrency.displayText
    )
}
